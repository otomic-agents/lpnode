package com.bytetrade.obridge.component.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import com.bytetrade.obridge.db.redis.RedisLocalDb;

@Slf4j
@Service
public class InitSwapEventService {
    private static final String INIT_SWAP_EVENT_KEY_PREFIX = "init:swap:event:";
    private static final long EXPIRE_TIME_DAYS = 3;

    @Autowired
    private RedisLocalDb redisLocalDb;

    /**
     * Add an initialization swap event to Redis and set an expiration time.
     *
     * @param eventBusinessId The business ID of the event.
     * @param value           The boolean value associated with the event.
     */
    public void addInitSwapEvent(String eventBusinessId, Boolean value) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = INIT_SWAP_EVENT_KEY_PREFIX + eventBusinessId;
        redisTemplate.opsForValue().set(key, value.toString(), EXPIRE_TIME_DAYS, TimeUnit.DAYS);
        log.info("Added initialization swap event to Redis with a 3-day expiration: {}", eventBusinessId);
    }

    /**
     * Check if an initialization swap event exists.
     *
     * @param eventBusinessId The business ID of the event to check.
     * @return true if the event exists, otherwise false.
     */
    public boolean checkInitSwapEvent(String eventBusinessId) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = INIT_SWAP_EVENT_KEY_PREFIX + eventBusinessId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Remove an initialization swap event from Redis.
     *
     * @param eventBusinessId The business ID of the event to remove.
     * @return true if the event was removed, false if the event does not exist.
     */
    public boolean removeInitSwapEvent(String eventBusinessId) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = INIT_SWAP_EVENT_KEY_PREFIX + eventBusinessId;
        Boolean result = redisTemplate.delete(key);
        boolean removed = Boolean.TRUE.equals(result);
        if (removed) {
            log.info("Removed initialization swap event from Redis: {}", eventBusinessId);
        } else {
            log.warn("Initialization swap event not found in Redis: {}", eventBusinessId);
        }
        return removed;
    }
}
