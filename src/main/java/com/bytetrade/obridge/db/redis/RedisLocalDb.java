package com.bytetrade.obridge.db.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisLocalDb {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password:}") // Optional password
    private String password;

    private RedisTemplate<String, Object> redisTemplate;
    private RedisTemplate<String, String> stringRedisTemplate;


    @PostConstruct
    public void init() {
        try {
            log.info("Initializing RedisLocalDb with host: {}, port: {}", host, port);
            LettuceConnectionFactory connectionFactory = createConnectionFactory();
            connectionFactory.afterPropertiesSet(); // Initialize connection factory
            connectionFactory.start(); // Start connection factory

            redisTemplate = createRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet(); // Initialize RedisTemplate

            stringRedisTemplate = createStringRedisTemplate(connectionFactory);
            stringRedisTemplate.afterPropertiesSet(); // Initialize StringRedisTemplate
            
            // Test connection
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection established successfully");


            
        } catch (Exception e) {
            log.error("Failed to initialize RedisLocalDb", e);
            throw new RuntimeException("Redis initialization failed", e);
        }
    }

    private LettuceConnectionFactory createConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        return factory;
    }

    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Set serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
    private RedisTemplate<String, String> createStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        return template;
    }

    public RedisTemplate<String, Object> get() {
        return redisTemplate;
    }
    public RedisTemplate<String, String> getStr() {
        return stringRedisTemplate;
    }
}
