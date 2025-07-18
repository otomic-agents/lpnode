package com.bytetrade.obridge.component.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import com.bytetrade.obridge.db.redis.RedisLocalDb;

@Slf4j
@Service
public class LockedBusinessService {
    private static final String LOCK_KEY_PREFIX = "business:locked:";
    private static final long EXPIRE_TIME_DAYS = 3;

    @Autowired
    private RedisLocalDb redisLocalDb;

    /**
     * Add business hash to locked list with 3 days expiration
     * 
     * @param businessHash business hash to be locked
     */
    public void addLockedBusiness(String businessHash) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = LOCK_KEY_PREFIX + businessHash;
        redisTemplate.opsForValue().set(key, "1", EXPIRE_TIME_DAYS, TimeUnit.DAYS);
        log.info("businessHash: {}", businessHash);
        log.info("Add business in cache with 3 days expiration: {}", businessHash);
    }

    /**
     * Check if business hash exists in locked list
     * 
     * @param businessHash business hash to check
     * @return true if business is locked, false otherwise
     */
    public boolean checkLockedBusiness(String businessHash) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = LOCK_KEY_PREFIX + businessHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Remove business hash from locked list
     * 
     * @param businessHash business hash to be removed
     * @return true if business was removed, false if it didn't exist
     */
    public boolean removeLockedBusiness(String businessHash) {
        RedisTemplate<String, Object> redisTemplate = redisLocalDb.get();
        String key = LOCK_KEY_PREFIX + businessHash;
        Boolean result = redisTemplate.delete(key);
        boolean removed = Boolean.TRUE.equals(result);
        if (removed) {
            log.info("Removed business from locked list: {}", businessHash);
        } else {
            log.warn("Business not found in locked list: {}", businessHash);
        }
        return removed;
    }
}
