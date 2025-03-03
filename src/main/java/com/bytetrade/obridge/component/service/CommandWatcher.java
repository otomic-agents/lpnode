package com.bytetrade.obridge.component.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.component.AtomicLPController;
import com.bytetrade.obridge.component.CommLpController;
import com.bytetrade.obridge.component.SingleSwapLpController;
import com.bytetrade.obridge.bean.AtomicBusinessFullData;
import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.db.redis.RedisConfig;
import java.util.Collection;

@Slf4j
@Service
public class CommandWatcher {

    List<RedisConnection> connections = new ArrayList<RedisConnection>();

    @Autowired
    ObjectMapper objectMapper;

    @Resource
    RedisConfig redisConfig;

    static RedisConnection redisConnection;
    @Autowired
    private ExecutorService exePoolService;
    private byte[][] listenChannels;
    @Lazy
    @Autowired
    private AtomicLPController atomicLPController;
    @Lazy
    @Autowired
    private CommLpController commLpController;
    @Lazy
    @Autowired
    private SingleSwapLpController singleSwapLpController;

    public void exitWatch() {
        log.info("Close the existing redis connection.");
        try {
            for (RedisConnection rc : connections) {
                // retrieve list of currently subscribed channels
                if (rc.getSubscription() != null) {
                    log.info("close ..................");
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
                    Thread.sleep(1000 * 60 * 5);
                }
            } catch (Exception runTimeErr) {
                log.info(runTimeErr.toString());
                System.out.println("Task was interrupted.");
            }
        };
        exePoolService.submit(printKeysTask);
    }

    public void updateWatch(byte[][] channels) {
        this.listenChannels = channels;

        this.exitWatch();
    }

    @Async
    public void watchCmds() {
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
                    boolean watchResult = doWatch(rc);
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

    public boolean doWatch(RedisConnection rc) {
        try {
            rc.subscribe(new MessageListener() {
                @Override
                public void onMessage(Message message, byte[] pattern) {
                    exePoolService.submit(() -> {
                        long currentThreadId = Thread.currentThread().getId();
                        long startTime = System.nanoTime();
                        CommandWatcher.this.notify(message);
                        long endTime = System.nanoTime();
                        long elapsedTimeMs = (endTime - startTime) / 1_000_000;
                        if (elapsedTimeMs > 1000) {
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
    private void notify(Message message) {
        String msg = new String(message.getBody());
        String channel = new String(message.getChannel());
        // CmdEvent cmdEvent;
        LPBridge lpBridge = atomicLPController.getBridgeFromChannel(channel);
        if ("SYSTEM_PING_CHANNEL".equals(channel)) {
            return;
        }
        // log.info("<-Message:" + msg);
        try {
            CmdEvent<?> cmdEvent = objectMapper.readValue(msg, new TypeReference<CmdEvent<?>>() {
            });
            if (!"CMD_UPDATE_QUOTE".equals(cmdEvent.getCmd())) {
                log.info("<-Message:" + msg);
                log.info("<-channel:" + channel);
                log.info("<-lpBridge:{} ,relayApiKey:{}", lpBridge.getMsmqName(), lpBridge.getRelayApiKey());
            }
            switch (cmdEvent.getCmd()) {
                case CmdEvent.CMD_UPDATE_QUOTE:
                    CmdEvent<AtomicBusinessFullData> atomicCmdEvent = objectMapper.readValue(msg,
                            new TypeReference<CmdEvent<AtomicBusinessFullData>>() {
                            });
                    commLpController.updateQuoteToRelay(atomicCmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.EVENT_ASK_REPLY:
                    commLpController.askReplyToRelay(cmdEvent.getCid(), cmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.CALLBACK_LOCK_QUOTE:
                    commLpController.newQuoteCallback(
                            cmdEvent.getPreBusiness().getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE,
                            cmdEvent);
                    break;
                case CmdEvent.CMD_TRANSFER_IN:
                    CmdEvent<AtomicBusinessFullData> atomicTransferInCmdEvent = objectMapper.readValue(msg,
                            new TypeReference<CmdEvent<AtomicBusinessFullData>>() {
                            });
                    atomicLPController.doTransferIn(
                            (AtomicBusinessFullData) atomicTransferInCmdEvent.getBusinessFullData(),
                            lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_CONFIRM:
                    CmdEvent<AtomicBusinessFullData> atomicTransferInConfirmCmdEvent = objectMapper.readValue(msg,
                            new TypeReference<CmdEvent<AtomicBusinessFullData>>() {
                            });
                    atomicLPController.doTransferInConfirm(
                            (AtomicBusinessFullData) atomicTransferInConfirmCmdEvent.getBusinessFullData(),
                            lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_REFUND:
                    CmdEvent<AtomicBusinessFullData> atomicTransferInRefundCmdEvent = objectMapper.readValue(msg,
                            new TypeReference<CmdEvent<AtomicBusinessFullData>>() {
                            });
                    atomicLPController.doTransferInRefund(
                            (AtomicBusinessFullData) atomicTransferInRefundCmdEvent.getBusinessFullData(),
                            lpBridge);
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