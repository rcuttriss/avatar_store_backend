package com.avatarstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Stripe webhook: allow any origin, no credentials (server-to-server)
        CorsConfiguration webhookConfig = new CorsConfiguration();
        webhookConfig.addAllowedOriginPattern("*");
        webhookConfig.addAllowedHeader("*");
        webhookConfig.addAllowedMethod("POST");
        source.registerCorsConfiguration("/purchases/webhook", webhookConfig);

        // Browser-facing endpoints: restrict to localhost origins
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        // Expose headers that the frontend needs to read
        // Content-Disposition is needed for file downloads to get the filename
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Content-Length");
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

