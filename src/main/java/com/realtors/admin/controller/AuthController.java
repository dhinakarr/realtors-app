package com.realtors.admin.controller;

import com.realtors.common.ApiResponse;
import com.realtors.common.util.JwtUtil;

import io.jsonwebtoken.Claims;

import com.realtors.admin.dto.LoginResponse;
import com.realtors.admin.service.TokenCacheService;
import com.realtors.admin.service.UserAuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserAuthService userAuthService;
    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;
    
    public AuthController(JwtUtil jwtUtil, TokenCacheService tokenCacheService, UserAuthService userAuthService) {
    	this.jwtUtil = jwtUtil;
    	this.tokenCacheService = tokenCacheService;
    	this.userAuthService = userAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Email and password are required", HttpStatus.BAD_REQUEST));
        }
        try {
            LoginResponse token = userAuthService.login(email, password);
            return ResponseEntity.ok(ApiResponse.success("Login successful", token, HttpStatus.OK));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage(), HttpStatus.UNAUTHORIZED));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
        	return ResponseEntity.badRequest().body(ApiResponse.failure("Unauthorized Access no allowed", HttpStatus.UNAUTHORIZED));
        }
        Claims claims = jwtUtil.extractClaims(token);
        String userId = claims.get("userId", String.class);
        boolean valid = tokenCacheService.isAccessTokenValid(userId, token);
        
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "claims", claims
        ));
    }
    
    /**
     * Refresh: provide userId + refreshToken -> if valid, issue new access token and replace in cache.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String refreshToken = req.get("refreshToken");
        if (userId == null || refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and refreshToken required"));
        }

        // quick signature & expiry check first
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        // ensure the refresh token matches what we have cached
        if (!tokenCacheService.isRefreshTokenValid(userId, refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token not recognized"));
        }
        Claims claims = jwtUtil.extractClaims(refreshToken);
        UUID roleId = (UUID) claims.get("roleId");
        // Build new access token (email & role can be fetched from DB or existing refresh claims)
        String email = req.get("email"); 
        String newAccessToken = jwtUtil.generateToken(email, userId, roleId);

        // update cached pair (reuse same refresh token)
        tokenCacheService.storeTokens(userId, newAccessToken, refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String header,
            @RequestBody Map<String, String> req)  {
        String token = header.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.failure("Unauthorized", HttpStatus.UNAUTHORIZED));
        }
        String userId = jwtUtil.extractClaims(token).get("userId", String.class);
        String oldPassword = req.get("oldPassword");
        String newPassword = req.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Old and new password required", HttpStatus.BAD_REQUEST));
        }

        userAuthService.changePassword(UUID.fromString(userId), oldPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null, HttpStatus.OK));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Email is required", HttpStatus.BAD_REQUEST));
        }
        userAuthService.generateResetToken(email);

        return ResponseEntity.ok(ApiResponse.success("Reset link sent", null, HttpStatus.OK));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        String token = req.get("token");
        String newPassword = req.get("newPassword");

        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Token and new password required", HttpStatus.BAD_REQUEST));
        }
        userAuthService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null, HttpStatus.OK));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
        	return ResponseEntity.badRequest().body(ApiResponse.failure("Unauthorized Access no allowed", HttpStatus.UNAUTHORIZED));
        }
        Claims claims = jwtUtil.extractClaims(token);
        tokenCacheService.evictToken(claims.get("userId", String.class));

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
