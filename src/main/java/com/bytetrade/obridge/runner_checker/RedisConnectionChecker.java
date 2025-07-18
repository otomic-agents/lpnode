package com.bytetrade.obridge.runner_checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedisConnectionChecker implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionChecker.class);

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:5000}")
    private int timeout;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Checking Redis connection during application startup...");
        
        int maxRetries = 10;
        int retryCount = 0;
        int retryDelayMs = 5000;
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        String pwd = password != null && !password.trim().isEmpty() ? password : null;
        
        try (JedisPool jedisPool = new JedisPool(poolConfig, host, port, timeout, pwd, database)) {
            while (retryCount < maxRetries) {
                try (Jedis jedis = jedisPool.getResource()) {
                    String response = jedis.ping();
                    if ("PONG".equals(response)) {
                        logger.info("Successfully connected to Redis at {}:{}, database: {}", 
                                   host, port, database);
                        return;
                    } else {
                        logger.warn("Unexpected Redis response: {}", response);
                    }
                } catch (JedisConnectionException e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        logger.error("FATAL: Failed to connect to Redis after {} attempts. Application will exit.", 
                                    maxRetries, e);
                        System.exit(1);
                    }
                    
                    logger.warn("Failed to connect to Redis (attempt {}/{}), retrying in {} ms", 
                               retryCount, maxRetries, retryDelayMs, e);
                    
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Redis connection check interrupted. Application will exit.", ie);
                        System.exit(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("FATAL: Error creating Redis connection pool. Application will exit.", e);
            System.exit(1);
        }
    }
}
