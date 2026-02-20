package com.avatarstore.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("Stripe secret key is not configured. Payment features will not work. "
                    + "Set STRIPE_SECRET_KEY environment variable or stripe.secret-key in application.properties.");
            return;
        }
        Stripe.apiKey = secretKey;
        log.info("Stripe API key configured successfully.");
    }
}
