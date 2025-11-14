package com.realtors.common;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    @Autowired
    private TokenCacheService tokenService;
    private JwtUtil jwtUtil;
    
    public JwtFilter(TokenCacheService tokenService, JwtUtil jwtUtil) {
    	this.tokenService = tokenService;
    	this.jwtUtil = jwtUtil;
    }

    private static final List<String> EXCLUDE_PREFIXES = List.of(
            "/api/auth/",
            "/public/"
    );
    
    // List of endpoints to skip
    private static final List<String> EXCLUDE_URLS = List.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/forgot-password"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return EXCLUDE_PREFIXES.stream().anyMatch(path::startsWith);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip excluded paths
        if (EXCLUDE_URLS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }
        
        try {
            Claims claims = jwtUtil.extractClaims(token);
            String user_id = claims.get("userId", String.class);
            String roleId = claims.get("roleId", String.class);

         // ✅ Check cache presence first
            if (!tokenService.containsKey(user_id)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Session expired or token not found");
                return;
            }

            // ✅ Then validate match (optional)
            boolean valid = tokenService.isAccessTokenValid(user_id, token);
            
            if (!valid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token");
                return;
            }
            
            // Attach claims to request for downstream services
            request.setAttribute("userId", user_id);
            request.setAttribute("roleId", roleId);
            request.setAttribute("email", claims.get("email"));

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}


