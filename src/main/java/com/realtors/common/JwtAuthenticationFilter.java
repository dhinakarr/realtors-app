package com.realtors.common;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.service.AuditContext;
import com.realtors.common.util.JwtUtil;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.dto.UserRole;

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
import java.util.Set;
import java.util.UUID;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final TokenCacheService tokenCacheService;
	private final List<String> excludeUrls;
	private final AntPathMatcher matcher = new AntPathMatcher();

	public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenCacheService tokenCacheService) {
		this.jwtUtil = jwtUtil;
		this.tokenCacheService = tokenCacheService;

		this.excludeUrls = List.of("/api/auth/**", "/api/public/**", "/api/projects/file/**", "/", "/index.html",
				"/favicon.ico", "/assets/**", "/**/*.js", "/**/*.css", "/**/*.png", "/error");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return excludeUrls.stream().anyMatch(pattern -> matcher.match(pattern, uri));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String authHeader = request.getHeader("Authorization");

		try {
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				String token = authHeader.substring(7);

				if (jwtUtil.validateToken(token) && !tokenCacheService.isRevoked(token)) {

					Claims claims = jwtUtil.extractClaims(token);
					String userId = claims.get("userId", String.class);

					UUID userUuid = UUID.fromString(claims.get("userId", String.class));
					String roleCode = claims.get("roleCode", String.class);
					UserRole role = UserRole.from(roleCode);
					
					UserPrincipalDto principal = new UserPrincipalDto(userUuid, Set.of(role));
					principal.setUserId(userUuid);
					principal.setRoles(Set.of(role));

					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

					SecurityContextHolder.getContext().setAuthentication(auth);

					AuditContext.setContext(request.getRemoteAddr(), request.getHeader("User-Agent"),
							UUID.fromString(userId));
				} else {
					log.warn("JWT token invalid or revoked");
				}
			}
		} catch (Exception e) {
			log.warn("JWT parsing failed: {}", e.getMessage());
		}

		// 🔥 ALWAYS continue the chain
		chain.doFilter(request, response);

		// 🔥 Clear audit after request completes
		AuditContext.clear();
	}

	/*
	 * private String getClientIp(HttpServletRequest request) { // Prioritize
	 * X-Forwarded-For (for cloud/proxy environments) String ip =
	 * request.getHeader("X-Forwarded-For"); if (ip == null || ip.isEmpty() ||
	 * "unknown".equalsIgnoreCase(ip)) { ip = request.getRemoteAddr(); } return ip;
	 * }
	 */
}
