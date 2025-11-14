package com.realtors.admin.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CacheManagerService {

    private final Cache<String, Object> generalCache;
    private final Cache<String, String> tokenCache;

    public CacheManagerService() {
        // General-purpose cache (static, lookup, CRUD data)
        this.generalCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        // Token cache (short-lived)
        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(500)
                .build();
    }

    // ===== General Cache Methods =====
    public <T> Optional<List<T>> getList(String cacheName, Object key, Class<T> type) {
        String fullKey = cacheName + "::" + key;
        Object val = generalCache.getIfPresent(fullKey);
        if (val instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<T> list = (List<T>) val;
            return Optional.of(list);
        }
        return Optional.empty();
    }
    
    public <T> Optional<T> get(String cacheName, Object key, Class<T> type) {
        String fullKey = cacheName + "::" + key;
        Object val = generalCache.getIfPresent(fullKey);
        return Optional.ofNullable(type.cast(val));
    }

    public void put(String cacheName, Object key, Object value) {
        generalCache.put(cacheName + "::" + key, value);
    }

    public void evict(String cacheName, Object key) {
        generalCache.invalidate(cacheName + "::" + key);
    }

    public void clear(String cacheName) {
        generalCache.asMap().keySet()
                .removeIf(k -> k.toString().startsWith(cacheName + "::"));
    }

    // ===== Token Cache Methods =====
    public void cacheToken(String username, String token) {
        tokenCache.put(username, token);
    }

    public Optional<String> getToken(String username) {
        return Optional.ofNullable(tokenCache.getIfPresent(username));
    }

    public void removeToken(String username) {
        tokenCache.invalidate(username);
    }

    public void clearAllTokens() {
        tokenCache.invalidateAll();
    }
}
