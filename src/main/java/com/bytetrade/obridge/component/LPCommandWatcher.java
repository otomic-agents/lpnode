package com.bytetrade.obridge.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.component.LPController;
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
    static byte[][] lastChannels;
    static LPController lastLpController;

    public void exitWatch() {
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
        }
        connections.clear();
    }

    @Async
    public void watchCmd(LPBridge lpBridge, LPController lpController) {
        RedisConnectionFactory rcf = redisConfig.getRedisTemplate().getConnectionFactory();
        RedisConnection rc = rcf.getConnection();
        connections.add(rc);

        // log.info("rcf={}, conn={}", rcf, rc);

        rc.subscribe(new MessageListener() {

            @Override
            public void onMessage(Message message, byte[] pattern) {
                long currentThreadId = Thread.currentThread().getId();
                log.info("Processing message in thread with ID: {}", currentThreadId);
                // log.info("message={}, {}", new String(message.getBody()), new
                // String(message.getChannel()));
                String msg = new String(message.getBody());
                CmdEvent cmdEvent;
                try {
                    log.info("Message:" + msg);
                    cmdEvent = objectMapper.readValue(msg, CmdEvent.class);
                    switch (cmdEvent.getCmd()) {
                        case CmdEvent.CMD_UPDATE_QUOTE:
                            lpController.updateQuote(cmdEvent.getQuoteData(), lpBridge);
                            break;
                        case CmdEvent.EVENT_ASK_REPLY:
                            lpController.askReply(cmdEvent.getCid(), cmdEvent.getQuoteData(), lpBridge);
                            break;
                        case CmdEvent.CALLBACK_LOCK_QUOTE:
                            lpController.newCallback(
                                    cmdEvent.getPreBusiness().getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE, cmdEvent);
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, lpBridge.getMsmqName().getBytes());
    }

    @Async
    public void watchCmds(byte[][] channels, LPController lpController) {
        int retries = 1;
        long threadId = Thread.currentThread().getId();
        while (true) {
            try {
                RedisConnectionFactory rcf = redisConfig.getRedisTemplate().getConnectionFactory();
                RedisConnection rc = rcf.getConnection();
                log.warn("Retry watchCmds (" + (retries) + ") threadId: " + threadId);
                connections.add(rc);
                log.info("channels:" + channels.length);
                if (channels.length > 0) {
                    boolean watchResult = doWatch(rc, lpController, channels);
                    if (watchResult) {
                        log.info("exit threadId:" + threadId);
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                log.error("watchCmds Exception:", e);
                try {
                    log.info("retry after 3 seconds");
                    Thread.sleep(3000);
                    retries++;
                } catch (InterruptedException se) {
                    se.printStackTrace();
                }
            }
        }
    }

    @Retryable(value = { Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public boolean doWatch(RedisConnection rc, LPController lpController, byte[][] channels) {
        try {

            if (redisConnection != null) {
                redisConnection.close();
            }

            redisConnection = rc;
            lastChannels = channels;
            lastLpController = lpController;

            rc.subscribe(new MessageListener() {

                @Override
                public void onMessage(Message message, byte[] pattern) {
                    LPCommandWatcher.this.notify(message, lpController);
                }

            }, channels);
            log.info("subscribe exit");
            return true;
        } catch (Exception e) {
            log.error("doWatch error:" + e);
            // watchCmds(channels, lpController);
            throw e;
        }
    }

    // @Scheduled(fixedRate = 30000)
    public void sendPingCommand() {
        log.info("sendPingCommand------->watcher connection " + (redisConnection == null));
        if (redisConnection != null) {
            try {
                String response = redisConnection.ping();
                log.info("response:" + response);
            } catch (Exception e) {
                log.info("-------->restart command watcher<--------");
                watchCmds(lastChannels, lastLpController);
            }
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
            log.info("Message:" + msg);
            log.info("channel:" + channel);
            log.info("lpBridge:" + lpBridge);
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}