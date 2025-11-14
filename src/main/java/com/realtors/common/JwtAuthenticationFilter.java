package com.realtors.common;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Validates JWT, sets SecurityContext. Skips configured public endpoints.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;
    private final List<String> excludeUrls;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, List<String> excludeUrls, TokenCacheService tokenCacheService) {
        this.jwtUtil = jwtUtil;
        this.excludeUrls = excludeUrls != null ? excludeUrls : List.of();
        this.tokenCacheService = tokenCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

    	String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7).trim();

        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        Claims claims = jwtUtil.validateAndExtractClaims(token);
        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token missing userId claim");
            return;
        }
        
        if (!tokenCacheService.isAccessTokenValid(userIdStr, token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token not present in cache or mismatched (session invalid)");
            return;
        }
        
        String email = claims.get("email", String.class);
        String role = claims.get("roleId", String.class);
        
     // ✅ Set SecurityContext
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userIdStr, // principal (you could also use UUID.fromString(userIdStr))
                        null,
                        role != null
                        		? List.of(new SimpleGrantedAuthority(role))
                                : Collections.emptyList()
                );

        authentication.setDetails(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
     // optionally set attributes for controllers/services
        request.setAttribute("userId", userIdStr);
        request.setAttribute("email", email);
        request.setAttribute("roleId", claims.get("roleId"));

        filterChain.doFilter(request, response);
    }
    
    private boolean shouldSkip(String path) {
    	return excludeUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

