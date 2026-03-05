package com.avatarstore.service;

import com.avatarstore.model.AvatarVersionPair;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.success-url:}")
    private String successUrl;

    @Value("${stripe.cancel-url:}")
    private String cancelUrl;

    /**
     * Creates a Stripe Checkout Session for one or more avatar version purchases.
     * Prices are read server-side from AvatarVersion — never trusted from the client.
     */
    public Session createCheckoutSession(List<AvatarVersionPair> items, UUID userId) throws StripeException {
        long totalInCents = 0;

        String versionIdsStr = items.stream()
                .map(i -> i.version().getId().toString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", userId.toString())
                .putMetadata("versionIds", versionIdsStr);

        for (AvatarVersionPair item : items) {
            long priceInCents = item.version().getPrice()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
            totalInCents += priceInCents;

            paramsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(priceInCents)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.avatar().getName() + " - " + item.version().getName())
                                                            .setDescription(item.version().getDescription())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        Session session = Session.create(paramsBuilder.build());
        log.info("Created Stripe Checkout Session: sessionId={}, userId={}, versionIds={}, total={}",
                session.getId(), userId, versionIdsStr, totalInCents);
        return session;
    }

    /**
     * Verifies a Stripe webhook event using the signature header.
     * This ensures the event genuinely came from Stripe and was not forged.
     *
     * @param payload   the raw request body (must be the exact bytes Stripe sent)
     * @param sigHeader the Stripe-Signature header value
     * @return the verified Event object
     * @throws SignatureVerificationException if the signature is invalid or the timestamp is too old
     */
    public Event constructWebhookEvent(String payload, String sigHeader) throws SignatureVerificationException {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "Stripe webhook secret is not configured. Set STRIPE_WEBHOOK_SECRET.");
        }
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
