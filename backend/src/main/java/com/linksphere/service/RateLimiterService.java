package com.linksphere.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Checks if a key has exceeded max requests in the given window.
     * Uses atomic Redis INCR with automatic expiration.
     *
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "ratelimit:" + key;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(redisKey, window);
            }
            return count != null && count <= maxRequests;
        } catch (Exception ex) {
            // Fail-open: If Redis is temporarily down, do not block legitimate user requests
            log.warn("Rate limiter failed for key: {}. Allowing request. Error: {}", redisKey, ex.getMessage());
            return true;
        }
    }
}
