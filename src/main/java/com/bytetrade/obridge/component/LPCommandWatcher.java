package com.bytetrade.obridge.component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Subscription;
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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 30;
    private static final long KEEP_ALIVE_TIME = 60L;
    @Autowired
    private ExecutorService exePoolService;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int QUEUE_CAPACITY = 20;

    private static ExecutorService executorService;

    static {
        executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TIME_UNIT,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY)
        );
    }

    private void exitWatch() {
        log.info("Close the existing redis connection.");
        for (RedisConnection rc : connections) {
            // retrieve list of currently subscribed channels
            rc.close();
            // if (rc.getSubscription() != null) {
            //     Collection<byte[]> subscribedChannels = rc.getSubscription().getChannels();
            //     if (subscribedChannels != null) {
            //         // unsubscribe from these channels one by one
            //         for (byte[] channel : subscribedChannels) {
            //             rc.getSubscription().unsubscribe(channel);
            //         }
            //     }
            // }
        }
        connections.clear();
    }
    @PostConstruct
    public void init() {
        exePoolService.submit(() -> {
            log.info("Periodically display the number of Redis items");
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            Runnable task = () -> {
                log.info("redis connect count = {}", connections.size());
                int index = 1;
                try {
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
                } catch (Exception e) {
                    log.info(e.getMessage());
                }

            };
            executor.scheduleAtFixedRate(task, 0, 15, TimeUnit.MINUTES);
        });

    }
    @Async
    public void watchCmds(byte[][] channels, LPController lpController) {
        int retries = 1;
        long threadId = Thread.currentThread().getId();
        while (true) {
            try {
                exitWatch();
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
                    log.info("retry watchCmds after 3 seconds");
                    Thread.sleep(3000);
                    retries++;
                } catch (InterruptedException se) {
                    se.printStackTrace();
                }
            }
        }
    }

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
                    executorService.submit(() -> {
                        long currentThreadId = Thread.currentThread().getId();
                        log.info("Processing message in thread with ID: {}", currentThreadId);
                        long startTime = System.nanoTime(); 
                        LPCommandWatcher.this.notify(message, lpController); 
                        long endTime = System.nanoTime(); 
                        long elapsedTimeMs = (endTime - startTime) / 1_000_000;
                        log.info("Time taken to execute notify: {} ms", elapsedTimeMs);
                    });
                }

            }, channels);
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
            log.info("message notify error:{}", e.getMessage());
            e.printStackTrace();
        }
    }
}