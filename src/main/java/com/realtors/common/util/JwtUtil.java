package com.realtors.common.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.service.RoleService;
import com.realtors.common.config.JwtProperties;

@Component
public class JwtUtil {
	
	private final JwtProperties jwtProperties;
	private RoleService roleService;
    private SecretKey key;

    public JwtUtil(JwtProperties jwtProperties, RoleService roleService) {
        this.jwtProperties = jwtProperties;
        this.roleService = roleService;
    }
    
    @PostConstruct
    public void init() {
    	if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()) {
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }
        // secret must be long enough for HS256 (recommend 256-bit min)
    	key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String userId, UUID roleId) {
    	
    	Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("userId", userId);
        
        if (roleId != null) {
            RoleDto role = roleService.findById(roleId)
                    .orElseThrow(() -> new IllegalStateException("Invalid roleId"));

            // 🔥 BUSINESS ROLE (for runtime auth)
            claims.put("roleCode", role.getFinanceRole()); // PM / PH / MD / FINANCE

            // Optional (audit / debugging)
            claims.put("roleId", roleId.toString());
        }
        
        claims.put("jti", UUID.randomUUID().toString());
    	
        long now = System.currentTimeMillis();
    	return Jwts.builder()
    			.setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Generate refresh token (longer-lived). Minimal claims; contains userId + jti.
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("jti", UUID.randomUUID().toString());

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("refresh-token")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtProperties.getRefreshExpirationMs()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Parse token and return claims or throw JwtException if invalid.
     */
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
    
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
    
    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public boolean validateToken(String token) {
        try {
        	validateAndExtractClaims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }
    
    /**
     * Extract userId claim (returns null if not present)
     */
    public String extractUserId(String token) {
        Claims claims = validateAndExtractClaims(token);
        return claims.get("userId", String.class);
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}

