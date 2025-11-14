package com.realtors.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public <T> void put(String cacheName, Object key, T value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            log.debug("Cached [{}] -> {}", cacheName, key);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, Object key, Class<T> clazz) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                return Optional.ofNullable((T) wrapper.get());
            }
        }
        return Optional.empty();
    }

    public void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted [{}] -> {}", cacheName, key);
        }
    }

    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared entire cache [{}]", cacheName);
        }
    }
}

