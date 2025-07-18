package com.bytetrade.obridge.component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import com.bytetrade.obridge.bean.OtmoicSystemEventBusMessage;
import com.bytetrade.obridge.bean.OtmoicSystemEventBusMessageReply;
import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction;
import com.bytetrade.obridge.component.chain_client_message_processer.ChainClientTransactionProcess;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class ChainMessageChannelListener {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password:}") // Optional password
    private String password;

    private JedisPool jedisPool;
    
    @Autowired
    private ExecutorService exePoolService;
    
    @Autowired
    private ChainClientTransactionProcess chainClientTransactionProcess;
    
    // Add running status flag
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        exePoolService.submit(() -> {
            try {
                listenMessage();
            } catch (Exception e) {
                log.error("Error in message listener thread", e);
            }
        });
        
        // Add a shutdown hook specific to this component
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down ChainMessageChannelListener");
            shutdown();
        }));
    }
    
    // Add shutdown method
    public void shutdown() {
        running = false;
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("JedisPool closed");
        }
    }

    private void initJedisPool() {
        if (jedisPool == null || jedisPool.isClosed()) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(0);
            poolConfig.setTestOnBorrow(true);

            if (password == null || password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            }

            log.info("Initialized Redis connection pool to {}:{}", host, port);
        }
    }
    
    public void listenMessage() {
        final String queueKey = "CHAIN_CLIENT_MESSAGE_CHANNEL_SEND";
        log.info("Starting to listen for messages on queue: {}", queueKey);

        initJedisPool();
        
        while (running && !Thread.currentThread().isInterrupted()) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                List<String> result = jedis.blpop(1, queueKey);
                
                if (result != null && result.size() == 2) {
                    String messageJson = result.get(1);
                    log.info("Received message from queue: {}", messageJson);
                    OtmoicSystemEventBusMessage message = JSON.parseObject(messageJson,
                            OtmoicSystemEventBusMessage.class);
                    log.info("Processing message: {}", message);

                    processMessage(message);
                }
            } catch (JedisConnectionException e) {
                log.error("Redis connection error: {}", e.getMessage());
                // Brief pause to avoid rapid cycling during connection issues
                sleepBriefly(1000);
            } catch (Exception e) {
                log.error("Error while listening for messages: {}", e.getMessage(), e);
                sleepBriefly(1000);
            } finally {
                // Ensure proper closure of Jedis connection
                if (jedis != null) {
                    try {
                        jedis.close();
                    } catch (Exception e) {
                        log.error("Error closing Jedis connection", e);
                    }
                }
            }
            
            // Additional check point for faster interrupt response
            if (!running || Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        // Ensure connection pool is closed before exiting
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }

        log.info("Message listener stopped for queue: {}", queueKey);
    }
    
    private void sleepBriefly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void processMessage(OtmoicSystemEventBusMessage message) {
        if (message.getType().equals("CHAIN_TRANSACTION")) {
            ChainTransaction chainTransaction = JSON.parseObject(JSON.toJSONString(message.getPayload()),
                    ChainTransaction.class);
            OtmoicSystemEventBusMessageReply reply = chainClientTransactionProcess.process(message.getMessageId(),
                    chainTransaction);
            if (reply != null) {
                sendReply(reply);
            }
            log.info("r transaction:{}", chainTransaction);
        }
    }

    private void sendReply(OtmoicSystemEventBusMessageReply replyMessage) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String chainClientReplyChannel = "CHAIN_CLIENT_MESSAGE_CHANNEL_SEND:" + replyMessage.getMessageId();
            String replyJson = JSON.toJSONString(replyMessage);
            
            Transaction transaction = jedis.multi();
            transaction.rpush(chainClientReplyChannel, replyJson);
            transaction.expire(chainClientReplyChannel, 86400);
            transaction.exec();
    
            log.info("Pushed reply to list {} with 24h expiration: {}", chainClientReplyChannel, replyJson);
        } catch (Exception e) {
            log.error("Error sending reply: {}", e.getMessage(), e);
        } finally {
            if (jedis != null) {
                try {
                    jedis.close();
                } catch (Exception e) {
                    log.error("Error closing Jedis connection", e);
                }
            }
        }
    }
}
