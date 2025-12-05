package com.realtors.common.config;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.JwtAuthenticationFilter;
import com.realtors.common.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtUtil jwtUtil, TokenCacheService tokenCacheService, CorsConfigurationSource corsConfigurationSource) {
        this.jwtUtil = jwtUtil;
        this.tokenCacheService = tokenCacheService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, tokenCacheService);

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
            		// permit API auth/public endpoints
            	    .requestMatchers("/api/auth/**", "/api/public/**", "/public/**","/files/**").permitAll()
            	    .requestMatchers("/api/projects/file/**").permitAll()
            	    .requestMatchers("/.well-known/**").permitAll()
            	    
            	 // SPA static files (safe)
            	    .requestMatchers(
            	        "/",                  // root index
            	        "/files/*",
            	        "/index.html",        // index file
            	        "/favicon.ico",       // favicon
            	        "/vite.svg",
            	        "/assets/**"          // JS, CSS, images
            	    ).permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
