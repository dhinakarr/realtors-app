package com.realtors.admin.controller;

import com.realtors.common.ApiResponse;
import com.realtors.common.util.JwtUtil;

import io.jsonwebtoken.Claims;

import com.realtors.admin.dto.AuthResponse;
import com.realtors.admin.dto.LoginResponse;
import com.realtors.admin.service.TokenCacheService;
import com.realtors.admin.service.UserAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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
			return ResponseEntity.badRequest()
					.body(ApiResponse.failure("Email and password are required", HttpStatus.BAD_REQUEST));
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
			return ResponseEntity.badRequest()
					.body(ApiResponse.failure("Unauthorized Access no allowed", HttpStatus.UNAUTHORIZED));
		}
		Claims claims = jwtUtil.extractClaims(token);
		String userId = claims.get("userId", String.class);
		boolean valid = tokenCacheService.isAccessTokenValid(userId, token);

		return ResponseEntity.ok(Map.of("valid", valid, "claims", claims));
	}

	/**
	 * Refresh: provide userId + refreshToken -> if valid, issue new access token
	 * and replace in cache.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestBody Map<String, String> req) {
		String userId = req.get("userId");
		String refreshToken = req.get("refreshToken");
		if (userId == null || refreshToken == null) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("userId and refreshToken required", HttpStatus.BAD_REQUEST));
		}
		// quick signature & expiry check first
		if (!jwtUtil.validateToken(refreshToken)) {
			return ResponseEntity.status(401).body(ApiResponse.failure("Invalid refresh token", HttpStatus.BAD_REQUEST));
		}
		// ensure the refresh token matches what we have cached
		if (!tokenCacheService.isRefreshTokenValid(userId, refreshToken)) {
			return ResponseEntity.status(401).body(ApiResponse.failure("Refresh token not recognized", HttpStatus.BAD_REQUEST));
		}
		Claims claims = jwtUtil.extractClaims(refreshToken);
		UUID roleId = (UUID) claims.get("roleId");
		// Build new access token (email & role can be fetched from DB or existing
		// refresh claims)
		String email = req.get("email");
		String newAccessToken = jwtUtil.generateToken(email, userId, roleId);

		// update cached pair (reuse same refresh token)
		tokenCacheService.storeTokens(userId, newAccessToken, refreshToken);
		return ResponseEntity.ok(ApiResponse.success("New token generated", Map.of("accessToken", newAccessToken), HttpStatus.OK));
	}

	@PostMapping("/change-password")
	public ResponseEntity<ApiResponse<String>> changePassword(@RequestHeader("Authorization") String header,
			@RequestBody Map<String, String> req) {
		String token = header.substring(7);

		if (!jwtUtil.validateToken(token)) {
			return ResponseEntity.status(401).body(ApiResponse.failure("Unauthorized", HttpStatus.UNAUTHORIZED));
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
	public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> req) {
		String email = req.get("email");

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("Email is required", HttpStatus.BAD_REQUEST));
		}

		// silently process (don't reveal existence)
		try {
			userAuthService.generateResetToken(email);
		} catch (Exception ignored) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("Email is required", HttpStatus.BAD_REQUEST));
		}

		return ResponseEntity.ok(
				ApiResponse.success("If the email is registered, a reset link has been sent.", null, HttpStatus.OK));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@RequestBody Map<String, String> req) {
		String token = req.get("token");
		String newPassword = req.get("newPassword");

		if (token == null || newPassword == null) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.failure("Token and new password required", HttpStatus.BAD_REQUEST));
		}
		if (newPassword.length() < 8) {
			throw new IllegalArgumentException("Password must be at least 8 characters");
		}
//        userAuthService.resetPassword(token, newPassword);
		AuthResponse auth = userAuthService.resetPassword(token, newPassword);
		return ResponseEntity.ok(ApiResponse.success("Password reset successful", auth, HttpStatus.OK));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String header) {
		String token = header.substring(7);
		if (!jwtUtil.validateToken(token)) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.failure("Unauthorized Access no allowed", HttpStatus.UNAUTHORIZED));
		}
		Claims claims = jwtUtil.extractClaims(token);
		tokenCacheService.evictToken(claims.get("userId", String.class));

		return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null, HttpStatus.OK));
	}
}
