package com.realtors.common.config;

import com.realtors.admin.service.TokenCacheService;
import com.realtors.common.JwtAuthenticationFilter;
import com.realtors.common.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JwtUtil jwtUtil;
    private TokenCacheService tokenCacheService;

    public SecurityConfig(JwtUtil jwtUtil, TokenCacheService tokenCacheService) {
    	this.jwtUtil = jwtUtil;
    	this.tokenCacheService = tokenCacheService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        // patterns to skip (Ant patterns)
        List<String> excludes = List.of(
                "/api/auth/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/public/**"
        );
        return new JwtAuthenticationFilter(jwtUtil,  excludes, this.tokenCacheService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // APIs: disable CSRF (or configure properly)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
               .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/public/**").permitAll()
               .requestMatchers("/**").permitAll()
               .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
               .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
               .requestMatchers(HttpMethod.DELETE, "/api/**").permitAll()
               .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
            )
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable());

        // place our JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
