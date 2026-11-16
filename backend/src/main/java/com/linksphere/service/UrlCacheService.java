package com.linksphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linksphere.dto.url.CachedUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCacheService {

    private static final String KEY_PREFIX = "link:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<CachedUrl> get(String shortCode) {
        String key = buildKey(shortCode);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            CachedUrl cached = objectMapper.convertValue(value, CachedUrl.class);
            return Optional.of(cached);
        } catch (Exception ex) {
            log.warn("Redis lookup failed for key: {}. Falling back to PostgreSQL. Error: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String shortCode, CachedUrl cachedUrl) {
        put(shortCode, cachedUrl, DEFAULT_CACHE_TTL);
    }

    public void put(String shortCode, CachedUrl cachedUrl, Duration ttl) {
        String key = buildKey(shortCode);
        try {
            redisTemplate.opsForValue().set(key, cachedUrl, ttl);
        } catch (Exception ex) {
            log.warn("Failed to cache key: {} in Redis. Error: {}", key, ex.getMessage());
        }
    }

    public void evict(String shortCode) {
        String key = buildKey(shortCode);
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache for short code: {}", shortCode);
        } catch (Exception ex) {
            log.warn("Failed to evict key: {} from Redis. Error: {}", key, ex.getMessage());
        }
    }

    private String buildKey(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
