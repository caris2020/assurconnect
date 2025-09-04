package com.assurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Minimal, hardening-first security setup:
     * - Stateless
     * - CSRF disabled (no cookies/session used)
     * - CORS handled by existing CorsFilter
     * - Public endpoints explicitly allowed
     * - Admin endpoints require ROLE_ADMIN
     * - Everything else requires authentication (temporary via HTTP Basic)
     *
     * Note: This is a first step. Replace Basic with JWT or session auth later.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Health and static docs/tests if needed
                .requestMatchers("/actuator/health", "/api/health").permitAll()
                // Public download endpoints use validation codes, not auth
                .requestMatchers("/api/download/**").permitAll()
                // Auth endpoints (custom controllers)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/login", "/api/users/register", "/api/users/check-username/**").permitAll()
                // Password reset public endpoints if any
                .requestMatchers("/api/password/**").permitAll()
                // Report request flow: allow only specific POST endpoints publicly
                .requestMatchers(HttpMethod.POST, "/api/report-requests").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/report-requests/validate-code").permitAll()
                // Debug endpoints restricted to ADMIN
                .requestMatchers("/api/report-requests/debug/**").hasRole("ADMIN")
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Subscriptions admin endpoints require ADMIN
                .requestMatchers("/api/subscriptions/**").hasRole("ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .contentTypeOptions(contentTypeOptions -> {})
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
            );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
