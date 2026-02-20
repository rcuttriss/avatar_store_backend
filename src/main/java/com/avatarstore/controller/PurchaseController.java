package com.avatarstore.controller;

import com.avatarstore.config.SupabaseJwtHelper;
import com.avatarstore.dto.ApiResponse;
import com.avatarstore.model.Avatar;
import com.avatarstore.service.AvatarService;
import com.avatarstore.service.PurchaseService;
import com.avatarstore.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {

    private final SupabaseJwtHelper supabaseJwtHelper;
    private final PurchaseService purchaseService;
    private final AvatarService avatarService;
    private final StripeService stripeService;

    /**
     * Creates a Stripe Checkout Session for the given avatar.
     * The price is looked up server-side — the client only sends the avatar ID.
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, String>>> createCheckoutSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Long> body) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required."));
        }
        UUID userId = userIdOpt.get();

        Long avatarId = body.get("avatarId");
        if (avatarId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("avatarId is required."));
        }

        // Prevent duplicate purchases
        if (purchaseService.hasPurchased(userId, avatarId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("You have already purchased this avatar."));
        }

        Avatar avatar;
        try {
            avatar = avatarService.getAvatarById(avatarId);
        } catch (RuntimeException e) {
            log.warn("Avatar not found for checkout: avatarId={}", avatarId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Avatar not found."));
        }

        if (avatar.getPrice() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Avatar has no price configured."));
        }

        try {
            Session session = stripeService.createCheckoutSession(avatar, userId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("sessionUrl", session.getUrl())));
        } catch (StripeException e) {
            log.error("Stripe error during checkout: avatarId={}, user={}, error={}",
                    avatarId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment service error. Please try again later."));
        }
    }

    /**
     * Stripe webhook endpoint. Receives events directly from Stripe's servers.
     * The request is verified using the Stripe-Signature header — no JWT auth needed.
     * Purchases are recorded ONLY when payment is confirmed here.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = stripeService.constructWebhookEvent(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature.");
        } catch (IllegalStateException e) {
            log.error("Webhook secret not configured: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Webhook not configured.");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            handleCheckoutSessionCompleted(event);
        } else {
            log.debug("Ignoring Stripe event type: {}", event.getType());
        }

        // Always return 200 so Stripe doesn't retry events we intentionally ignore
        return ResponseEntity.ok("OK");
    }

    /**
     * Check whether the authenticated user has purchased a specific avatar.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPurchaseStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("avatarId") Long avatarId) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required."));
        }

        boolean purchased = purchaseService.hasPurchased(userIdOpt.get(), avatarId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("purchased", purchased)));
    }

    /**
     * Extracts userId and avatarId from the Stripe session metadata and records the purchase.
     * Idempotent: checks hasPurchased before inserting to handle Stripe retries safely.
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) {
            log.error("Could not deserialize checkout session from webhook event: {}", event.getId());
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("userId") || !metadata.containsKey("avatarId")) {
            log.error("Webhook session missing required metadata (userId/avatarId): sessionId={}",
                    session.getId());
            return;
        }

        UUID userId;
        Long avatarId;
        try {
            userId = UUID.fromString(metadata.get("userId"));
            avatarId = Long.parseLong(metadata.get("avatarId"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid metadata in webhook session: sessionId={}, error={}",
                    session.getId(), e.getMessage());
            return;
        }

        // Idempotency: skip if already recorded (Stripe may retry)
        if (purchaseService.hasPurchased(userId, avatarId)) {
            log.info("Purchase already recorded (idempotent skip): userId={}, avatarId={}, sessionId={}",
                    userId, avatarId, session.getId());
            return;
        }

        boolean recorded = purchaseService.recordPurchase(userId, avatarId, session.getId());
        if (recorded) {
            log.info("Purchase recorded via webhook: userId={}, avatarId={}, sessionId={}",
                    userId, avatarId, session.getId());
        } else {
            log.error("Failed to record purchase via webhook: userId={}, avatarId={}, sessionId={}",
                    userId, avatarId, session.getId());
        }
    }
}
