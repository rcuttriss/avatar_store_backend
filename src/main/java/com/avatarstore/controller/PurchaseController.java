package com.avatarstore.controller;

import com.avatarstore.config.SupabaseJwtHelper;
import com.avatarstore.dto.ApiResponse;
import com.avatarstore.dto.CheckoutRequest;
import com.avatarstore.dto.PurchasedItem;
import com.avatarstore.model.AvatarVersionPair;
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

import java.util.Arrays;
import java.util.List;
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
     * Creates a Stripe Checkout Session for one or more avatar versions.
     * Client sends a list of versionIds — prices are always looked up server-side.
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, String>>> createCheckoutSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CheckoutRequest body) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required."));
        }
        UUID userId = userIdOpt.get();

        List<Long> versionIds = body.versionIds();
        if (versionIds == null || versionIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("versionIds is required."));
        }

        List<AvatarVersionPair> items;
        try {
            items = versionIds.stream()
                    .map(vId -> {
                        var version = avatarService.getVersionById(vId);
                        var avatar = avatarService.getAvatarById(version.getAvatarId());
                        return new AvatarVersionPair(avatar, version);
                    })
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Version or avatar not found during checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("One or more versions not found."));
        }

        // Prevent duplicate purchases
        for (AvatarVersionPair item : items) {
            if (purchaseService.hasPurchased(userId, item.version().getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "You have already purchased: " + item.avatar().getName() + " - " + item.version().getName()));
            }
        }

        try {
            Session session = stripeService.createCheckoutSession(items, userId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("sessionUrl", session.getUrl())));
        } catch (StripeException e) {
            log.error("Stripe error during checkout: versionIds={}, user={}, error={}", versionIds, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Payment service error. Please try again later."));
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
     * Returns all purchases for the authenticated user with embedded avatar and version info.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PurchasedItem>>> getMyPurchases(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required."));
        }

        List<PurchasedItem> purchases = purchaseService.getPurchases(userIdOpt.get());
        return ResponseEntity.ok(ApiResponse.success(purchases, purchases.size()));
    }

    /**
     * Check whether the authenticated user has purchased a specific version.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPurchaseStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("versionId") Long versionId) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required."));
        }

        boolean purchased = purchaseService.hasPurchased(userIdOpt.get(), versionId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("purchased", purchased)));
    }

    /**
     * Extracts userId and versionIds from Stripe session metadata and records purchases.
     * Idempotent: skips versions already recorded to handle Stripe retries safely.
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            log.error("Could not deserialize checkout session from webhook event: {}", event.getId());
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("userId") || !metadata.containsKey("versionIds")) {
            log.error("Webhook session missing required metadata (userId/versionIds): sessionId={}", session.getId());
            return;
        }

        UUID userId;
        List<Long> versionIds;
        try {
            userId = UUID.fromString(metadata.get("userId"));
            versionIds = Arrays.stream(metadata.get("versionIds").split(","))
                    .map(Long::parseLong)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.error("Invalid metadata in webhook session: sessionId={}, error={}", session.getId(), e.getMessage());
            return;
        }

        // Filter out already-recorded versions (idempotency for Stripe retries)
        List<Long> newVersionIds = versionIds.stream()
                .filter(vId -> !purchaseService.hasPurchased(userId, vId))
                .toList();

        if (newVersionIds.isEmpty()) {
            log.info("All purchases already recorded (idempotent skip): userId={}, sessionId={}", userId, session.getId());
            return;
        }

        List<Long> avatarIds = newVersionIds.stream()
                .map(vId -> avatarService.getVersionById(vId).getAvatarId())
                .toList();

        boolean recorded = purchaseService.recordPurchases(userId, avatarIds, newVersionIds, session.getId());
        if (recorded) {
            log.info("Purchases recorded via webhook: userId={}, versionIds={}, sessionId={}", userId, newVersionIds, session.getId());
        } else {
            log.error("Failed to record purchases via webhook: userId={}, versionIds={}, sessionId={}", userId, newVersionIds, session.getId());
        }
    }
}
