package com.bytetrade.obridge.component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.db.redis.RedisConfig;
import java.util.Collection;

@Slf4j
@Service
public class LPCommandWatcher {

    List<RedisConnection> connections = new ArrayList<RedisConnection>();

    @Autowired
    ObjectMapper objectMapper;

    @Resource
    RedisConfig redisConfig;

    static RedisConnection redisConnection;
    @Autowired
    private ExecutorService exePoolService;
    private byte[][] listenChannels;
    private LPController lpController;

    public void exitWatch() {
        log.info("Close the existing redis connection.");
        try {
            for (RedisConnection rc : connections) {
                // retrieve list of currently subscribed channels
                if (rc.getSubscription() != null) {
                    Collection<byte[]> subscribedChannels = rc.getSubscription().getChannels();
                    if (subscribedChannels != null) {
                        // unsubscribe from these channels one by one
                        for (byte[] channel : subscribedChannels) {
                            rc.getSubscription().unsubscribe(channel);
                        }
                    }
                }
                rc.close();
            }
        } catch (Exception e) {
            log.info(e.toString());
        }
        try {
            connections.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void init() {
        Runnable printKeysTask = () -> {
            log.info("redis connect count = {}", connections.size());
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int index = 1;
                    for (RedisConnection rc : connections) {
                        Subscription subscription = rc.getSubscription();
                        if (subscription == null) {
                            log.info("redis connect {} ,Subscription channels:Empty ", index);
                            break;
                        }
                        Collection<byte[]> subscribedChannels = subscription.getChannels();
                        if (subscribedChannels != null) {
                            var k = 1;
                            for (byte[] channel : subscribedChannels) {
                                // System.out.println(new String(channel, StandardCharsets.UTF_8));
                                log.info("redis connect index {} ,Subscription channels {} , channel: {}", index, k,
                                        new String(channel, StandardCharsets.UTF_8));
                                k = k + 1;
                            }
                        }
                        index = index + 1;
                    }
                    Thread.sleep(1000*60*5);
                }
            } catch (Exception runTimeErr) {
                log.info(runTimeErr.toString());
                System.out.println("Task was interrupted.");
            }
        };
        exePoolService.submit(printKeysTask);
    }

    public void updateWatch(byte[][] channels, LPController lpController) {
        this.listenChannels = channels;
        this.lpController = lpController;
        this.exitWatch();
    }

    @Async
    public void watchCmds(LPController lpController) {
        int retries = 1;
        long threadId = Thread.currentThread().getId();
        for (;;) {
            try {
                log.info("execute watchCmds ,new channel size:{}", this.listenChannels.length);
                RedisConnectionFactory rcf = redisConfig.getRedisTemplate().getConnectionFactory();
                RedisConnection rc = rcf.getConnection();

                log.warn("watchCmds (" + (retries) + ") threadId: " + threadId);
                connections.add(rc);
                log.info("channels:" + this.listenChannels.length);
                if (this.listenChannels.length > 0) {
                    boolean watchResult = doWatch(rc, lpController);
                    if (watchResult) {
                        log.info("exit threadId:" + threadId);
                    }
                }
                log.info("retry watchCmds after 3 seconds ,watch exit");
            } catch (Exception e) {
                log.info("watchCmds Exception:", e);
                log.info("retry watchCmds after 3 seconds, watch error");
            } finally {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    log.info(e.toString());
                }
            }
        }
    }

    public boolean doWatch(RedisConnection rc, LPController lpController) {
        try {
            rc.subscribe(new MessageListener() {
                @Override
                public void onMessage(Message message, byte[] pattern) {
                    exePoolService.submit(() -> {
                        long currentThreadId = Thread.currentThread().getId();
                        long startTime = System.nanoTime();
                        LPCommandWatcher.this.notify(message, lpController);
                        long endTime = System.nanoTime();
                        long elapsedTimeMs = (endTime - startTime) / 1_000_000;
                        if (elapsedTimeMs>1000){
                            log.info("Processing message in thread with ID: {}  , Time taken to execute notify: {} ms",
                            currentThreadId, elapsedTimeMs);
                        }
                    });
                }

            }, this.listenChannels);
            log.info("subscribe exit");
            return true;
        } catch (Exception e) {
            log.error("doWatch error:" + e);
            throw e;
        }
    }

    @Async
    private void notify(Message message, LPController lpController) {
        String msg = new String(message.getBody());
        String channel = new String(message.getChannel());
        CmdEvent cmdEvent;
        LPBridge lpBridge = lpController.getBridgeFromChannel(channel);
        if ("SYSTEM_PING_CHANNEL".equals(channel)) {
            return;
        }
        try {
            log.info("<-Message:" + msg);
            log.info("<-channel:" + channel);
            log.info("<-lpBridge:{} ,relayApiKey:{}", lpBridge.getMsmqName(), lpBridge.getRelayApiKey());
            cmdEvent = objectMapper.readValue(msg, CmdEvent.class);
            switch (cmdEvent.getCmd()) {
                case CmdEvent.CMD_UPDATE_QUOTE:
                    lpController.updateQuote(cmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.EVENT_ASK_REPLY:
                    lpController.askReply(cmdEvent.getCid(), cmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.CALLBACK_LOCK_QUOTE:
                    lpController.newCallback(cmdEvent.getPreBusiness().getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE,
                            cmdEvent);
                    break;
                case CmdEvent.CMD_TRANSFER_IN:
                    lpController.transferIn(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_CONFIRM:
                    lpController.transferInConfirm(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_REFUND:
                    lpController.transferInRefund(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.info("message notify error:{}", e.getMessage());
            e.printStackTrace();
        }
    }
}