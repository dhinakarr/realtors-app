package com.realtors.admin.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TokenCacheService {

    private static final Logger log = LoggerFactory.getLogger(TokenCacheService.class);

    // Create a Caffeine cache for tokens
    private final Cache<String, Map<String, String>> tokenCache;

    public TokenCacheService() {
        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS) // access token cache TTL
                .maximumSize(10000)
                .build();
    }
    
    /**
     * Store access and refresh token for a user.
     */
    public void storeTokens(String userId, String accessToken, String refreshToken) {
        tokenCache.put(userId, Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    /**
     * Get access token for a user.
     */
    public Optional<String> getAccessToken(String userId) {
        Map<String, String> tokens = tokenCache.getIfPresent(userId);
        return tokens != null ? Optional.ofNullable(tokens.get("accessToken")) : Optional.empty();
    }

    /**
     * Get refresh token for a user.
     */
    public Optional<String> getRefreshToken(String userId) {
        Map<String, String> tokens = tokenCache.getIfPresent(userId);
        return tokens != null ? Optional.ofNullable(tokens.get("refreshToken")) : Optional.empty();
    }

    /**
     * Validate that the given token matches the cached one.
     */
    public boolean isAccessTokenValid(String userId, String accessToken) {
        return getAccessToken(userId)
                .map(token -> token.equals(accessToken))
                .orElse(false);
    }

    /**
     * Validate that the given refresh token matches the cached one.
     */
    public boolean isRefreshTokenValid(String userId, String refreshToken) {
        return getRefreshToken(userId)
                .map(token -> token.equals(refreshToken))
                .orElse(false);
    }

    public boolean containsKey(String userId) {
    	return tokenCache.asMap().containsKey(userId);
    }
    
    /**
     * Remove token from cache (e.g., on logout).
     */
    public void evictToken(String userId) {
        log.info("Evicting token for user: {}", userId);
        tokenCache.invalidate(userId);
    }

    /**
     * Clear all tokens (e.g., admin reset or shutdown).
     */
    public void clearAll() {
        log.warn("Clearing all tokens from cache!");
        tokenCache.invalidateAll();
    }
}

