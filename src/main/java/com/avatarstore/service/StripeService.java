package com.avatarstore.service;

import com.avatarstore.model.Avatar;
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
     * Creates a Stripe Checkout Session for a one-time avatar purchase.
     * The price is read from the Avatar model (server-side), never from the client.
     *
     * @param avatar the avatar being purchased (price comes from here)
     * @param userId the authenticated user's UUID (stored in session metadata for the webhook)
     * @return the Stripe Checkout Session
     */
    public Session createCheckoutSession(Avatar avatar, UUID userId) throws StripeException {
        long priceInCents = avatar.getPrice()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", userId.toString())
                .putMetadata("avatarId", avatar.getId().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(priceInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(avatar.getName())
                                                                .setDescription(avatar.getShortDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("Created Stripe Checkout Session: sessionId={}, userId={}, avatarId={}, amount={}",
                session.getId(), userId, avatar.getId(), priceInCents);
        return session;
    }

    public Session createCheckoutSession(List<Avatar> avatars, UUID userId) throws StripeException {
        long priceInCents = 0;
        for (Avatar a : avatars) {
            priceInCents += a.getPrice()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
        }

        String avatarIdsStr = avatars.stream()
                .map(a -> a.getId().toString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", userId.toString())
                .putMetadata("avatarIdsStr", avatarIdsStr);


        for (Avatar a : avatars) {
            // For multiple items, we can add multiple line items to the session.
            paramsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(a.getPrice().multiply(BigDecimal.valueOf(100)).longValueExact())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(a.getName())
                                                            .setDescription(a.getShortDescription())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }
        SessionCreateParams params = paramsBuilder.build();
        Session session = Session.create(params);
        log.info("Created Stripe Checkout Session: sessionId={}, userId={}, avatarIds={}, amount={}",
                session.getId(), userId, avatarIdsStr, priceInCents);
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
