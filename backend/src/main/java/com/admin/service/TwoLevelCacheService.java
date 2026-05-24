package com.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoLevelCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final Cache<String,CacheEnvelope> localCache = Caffeine.newBuilder()
            .maximumSize(3000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    private final ExecutorService refreshExecutor = Executors.newFixedThreadPool(4);

    public <T> T getIfPresent(String key, TypeReference<T> type){
        CacheEnvelope local = localCache.getIfPresent(key);
        if (local!=null && local.isLogicalValid()){
            return local.getNullValue() ? null : convert(local.getData(), type);
        }

        CacheEnvelope redis = readRedis(key);
        if (redis != null){
            localCache.put(key, redis);
            if (redis.isLogicalValid()){
                return redis.getNullValue() ? null : convert(redis.getData(), type);
            }
        }
        return null;
    }

    public <T> void putValue(String key, T data, Duration ttl){
        if (data == null){
            return;
        }
        CacheEnvelope envelope = CacheEnvelope.of(data, false, ttl);
        writeBoth(key, envelope, ttl.multipliedBy(3));
    }

    public <T> T get(String key, TypeReference<T> type, Duration logicalTtl, Duration nullTtl, Supplier<T> loader){
        CacheEnvelope local = localCache.getIfPresent(key);

        if (local!=null){
            if (local.isLogicalValid()){
                return convert(local.getData(), type);
            }
            refreshAsync(key, logicalTtl, nullTtl, loader);
            return local.getNullValue() ? null : convert(local.getData(), type);
        }

        CacheEnvelope redis = readRedis(key);

        if (redis != null){
            localCache.put(key, redis);
            if (redis.isLogicalValid()){
                return redis.getNullValue() ? null :convert(redis.getData(), type);
            }
            refreshAsync(key, logicalTtl, nullTtl, loader);
            return redis.getNullValue() ? null : convert(redis.getData(), type);
        }
        return rebuild(key, logicalTtl, nullTtl, loader);
    }

    public void evict(String key) {
        localCache.invalidate(key);      // 删除本地缓存
        redisTemplate.delete(key);        // 删除 Redis 缓存
    }

    public void evictByPrefix(String prefix) {
        localCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        var keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }


    private CacheEnvelope readRedis(String key) {
        try{
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null :objectMapper.readValue(json, CacheEnvelope.class);
        }catch (Exception e){
            log.error("read cache failed, key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    private <T> void refreshAsync(String key, Duration logicalTtl, Duration nullTtl, Supplier<T> loader) {
        String lockKey = "lock:" + key;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals( locked)) return;

        refreshExecutor.submit(() -> {
            try {
                rebuild(key, logicalTtl, nullTtl, loader);
            } catch (Exception e) {
                log.error("refresh cache failed, key={}, error={}", key, e.getMessage());
            }finally {
                redisTemplate.delete(lockKey);
            }
        });
    }

    private <T> T rebuild(String key, Duration logicalTtl, Duration nullTtl, Supplier<T> loader) {
        T data = loader.get();
        Boolean nullValue = data == null;
        Duration ttl = nullValue ? nullTtl : logicalTtl;
        CacheEnvelope envelope = CacheEnvelope.of(data, nullValue, ttl);
        writeBoth(key, envelope,ttl.multipliedBy(3));
        return data;
    }

    private void writeBoth(String key, CacheEnvelope envelope, Duration duration) {
        try {
            localCache.put(key, envelope);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(envelope), duration);
        }catch (Exception e){
            log.error("write cache failed, key={}, error={}", key, e.getMessage());
        }
    }

    private <T> T convert(Object data, TypeReference<T> type) {
        return objectMapper.convertValue(data, type);
    }

    @Data
    public static class CacheEnvelope{
        private Object data;
        private Boolean nullValue;
        private long logicalExpireAt;

        public static CacheEnvelope of(Object data, boolean nullValue, Duration ttl){
            CacheEnvelope e = new CacheEnvelope();
            e.data = data;
            e.nullValue = nullValue;
            e.logicalExpireAt =System.currentTimeMillis() + ttl.toMillis();
            return e;
        }

        public boolean isLogicalValid(){
            return System.currentTimeMillis() <logicalExpireAt;
        }
    }
}
