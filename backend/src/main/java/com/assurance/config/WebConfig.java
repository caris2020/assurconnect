package com.assurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Origines autorisées depuis variable d'env (virgule séparée). Fallback '*' en dev.
        String origins = System.getenv("ALLOWED_ORIGINS");
        if (origins != null && !origins.isBlank()) {
            for (String origin : origins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) config.addAllowedOrigin(trimmed);
            }
        } else {
            config.addAllowedOriginPattern("*");
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}


