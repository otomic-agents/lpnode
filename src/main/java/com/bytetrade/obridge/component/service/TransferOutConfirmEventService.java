package com.bytetrade.obridge.component.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import com.bytetrade.obridge.db.redis.RedisLocalDb;

@Slf4j
@Service
public class TransferOutConfirmEventService {
    private static final String TRANSFER_OUT_CONFIRM_EVENT_KEY_PREFIX = "transfer:out:confirm:event:";
    private static final long EXPIRE_TIME_DAYS = 3;

    @Autowired
    private RedisLocalDb redisLocalDb;

    /**
     * Add a transfer-out confirmation event to Redis and set an expiration time.
     *
     * @param eventBusinessId The business ID of the event.
     * @param value           The boolean value associated with the event.
     */
    public void addTransferOutConfirmEvent(String eventBusinessId, Boolean value) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = TRANSFER_OUT_CONFIRM_EVENT_KEY_PREFIX + eventBusinessId;
        redisTemplate.opsForValue().set(key, value.toString(), EXPIRE_TIME_DAYS, TimeUnit.DAYS);
        log.info("Added transfer-out confirmation event to Redis with a 3-day expiration: {}", eventBusinessId);
    }

    /**
     * Check if a transfer-out confirmation event exists.
     *
     * @param eventBusinessId The business ID of the event to check.
     * @return true if the event exists, otherwise false.
     */
    public boolean checkTransferOutConfirmEvent(String eventBusinessId) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = TRANSFER_OUT_CONFIRM_EVENT_KEY_PREFIX + eventBusinessId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Remove a transfer-out confirmation event from Redis.
     *
     * @param eventBusinessId The business ID of the event to remove.
     * @return true if the event was removed, false if the event does not exist.
     */
    public boolean removeTransferOutConfirmEvent(String eventBusinessId) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = TRANSFER_OUT_CONFIRM_EVENT_KEY_PREFIX + eventBusinessId;
        Boolean result = redisTemplate.delete(key);
        boolean removed = Boolean.TRUE.equals(result);
        if (removed) {
            log.info("Removed transfer-out confirmation event from Redis: {}", eventBusinessId);
        } else {
            log.warn("Transfer-out confirmation event not found in Redis: {}", eventBusinessId);
        }
        return removed;
    }
}
