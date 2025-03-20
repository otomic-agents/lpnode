package com.bytetrade.obridge.component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bytetrade.obridge.bean.OtmoicSystemEventBusMessage;
import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction;
import com.bytetrade.obridge.component.chain_client_message_processer.ChainClientTransactionProcess;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SystemEventBusListener {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password:}") // Optional password
    private String password;
    
    private JedisPool jedisPool;
    
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
        final String queueKey = "SYSTEM_EVENT_BUS_QUEUE";
        log.info("Starting to listen for messages on queue: {}", queueKey);
        
        initJedisPool();
        
        while (!Thread.currentThread().isInterrupted()) {
            try (Jedis jedis = jedisPool.getResource()) {
                List<String> result = jedis.blpop(5, queueKey);
                
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
                try {
                    if (jedisPool != null && !jedisPool.isClosed()) {
                        jedisPool.close();
                    }
                    jedisPool = null;
                    initJedisPool();
                    
                    Thread.sleep(1000); 
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("Error while listening for messages: {}", e.getMessage(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        
        log.info("Message listener stopped for queue: {}", queueKey);
    }
    
    private void processMessage(OtmoicSystemEventBusMessage message) {
        ChainTransaction chainTransaction =  JSON.parseObject(JSON.toJSONString(message.getPayload()), ChainTransaction.class) ;
        log.info("r transaction:{}", chainTransaction);
    }
}
