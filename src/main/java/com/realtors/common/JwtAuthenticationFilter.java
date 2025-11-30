package com.realtors.common;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;
    private final List<String> excludeUrls;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenCacheService tokenCacheService) {
        this.jwtUtil = jwtUtil;
        this.tokenCacheService = tokenCacheService;

        this.excludeUrls = List.of(
                "/api/auth/**",
                "/api/public/**",
                "/",
                "/index.html",
                "/favicon.ico",
                "/assets/**",
                "/**/*.js",
                "/**/*.css",
                "/**/*.png",
                "/error"
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return excludeUrls.stream().anyMatch(pattern -> matcher.match(pattern, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

//    	log.info("@JwtAuthenticationFilter Processing JWT for request: {}", request.getRequestURI());
    	
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
//            log.info("@JwtAuthenticationFilter Processing authHeader: {}", authHeader);
            try {
                if (jwtUtil.validateToken(token) && !tokenCacheService.isRevoked(token)) {
                    Claims claims = jwtUtil.extractClaims(token);
                    String userId = claims.get("userId", String.class);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, List.of());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("JWT token is invalid or revoked: {}", token);
                }
            } catch (Exception ex) {
                log.warn("JWT processing failed: {}", ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
