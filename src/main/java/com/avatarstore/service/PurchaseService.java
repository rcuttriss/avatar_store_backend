package com.avatarstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;

    /**
     * Returns true if the user has a purchase record for the given avatar.
     * Uses service role so RLS does not block the check.
     */
    public boolean hasPurchased(UUID userId, Long avatarId) {
        if (userId == null || avatarId == null) {
            return false;
        }
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/rest/v1/purchases?user_id=eq." + userId + "&avatar_id=eq." + avatarId + "&select=id&limit=1";
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().trim().isEmpty()) {
                log.debug("No purchase record for user={}, avatarId={}", userId, avatarId);
                return false;
            }
            List<Map<String, Object>> rows = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            boolean found = rows != null && !rows.isEmpty();
            if (!found) {
                log.debug("No purchase record for user={}, avatarId={}", userId, avatarId);
            }
            return found;
        } catch (Exception e) {
            log.warn("Failed to check purchase: user={}, avatarId={}", userId, avatarId, e);
            return false;
        }
    }

    /**
     * Inserts a purchase record without a Stripe session ID.
     * Delegates to the full method with a null session ID.
     */
    public boolean recordPurchase(UUID userId, Long avatarId) {
        return recordPurchase(userId, avatarId, null);
    }

    /**
     * Inserts a purchase record for the given user and avatar,
     * including the Stripe Checkout Session ID for audit trail.
     *
     * @param userId          the user's UUID (must exist in auth.users)
     * @param avatarId        the avatar id (must exist in avatars)
     * @param stripeSessionId the Stripe Checkout Session ID (nullable for non-Stripe flows)
     * @return true if the row was inserted, false on failure
     */
    public boolean recordPurchase(UUID userId, Long avatarId, String stripeSessionId) {
        if (userId == null || avatarId == null) {
            return false;
        }
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/rest/v1/purchases";
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        headers.set("Prefer", "return=minimal");

        String body;
        if (stripeSessionId != null && !stripeSessionId.isBlank()) {
            body = String.format(
                    "{\"user_id\":\"%s\",\"avatar_id\":%d,\"stripe_session_id\":\"%s\"}",
                    userId, avatarId, stripeSessionId);
        } else {
            body = String.format("{\"user_id\":\"%s\",\"avatar_id\":%d}", userId, avatarId);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Recorded purchase: user={}, avatarId={}, stripeSessionId={}",
                        userId, avatarId, stripeSessionId);
                return true;
            }
            log.warn("Failed to record purchase: status={}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.warn("Failed to record purchase: user={}, avatarId={}", userId, avatarId, e);
            return false;
        }
    }

    /**
     * Bulk-inserts purchase records for multiple avatars in a single Supabase call.
     * PostgREST accepts a JSON array, inserting all rows in one transaction.
     *
     * @param userId          the user's UUID
     * @param avatarIds       list of avatar IDs to record purchases for
     * @param stripeSessionId the Stripe Checkout Session ID (nullable)
     * @return true if all rows were inserted, false on failure
     */
    public boolean recordPurchaseList(UUID userId, List<Long> avatarIds, String stripeSessionId) {
        if (userId == null || avatarIds == null || avatarIds.isEmpty()) {
            return false;
        }
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/rest/v1/purchases";
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        headers.set("Prefer", "return=minimal");

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < avatarIds.size(); i++) {
            if (i > 0) sb.append(",");
            if (stripeSessionId != null && !stripeSessionId.isBlank()) {
                sb.append(String.format(
                        "{\"user_id\":\"%s\",\"avatar_id\":%d,\"stripe_session_id\":\"%s\"}",
                        userId, avatarIds.get(i), stripeSessionId));
            } else {
                sb.append(String.format(
                        "{\"user_id\":\"%s\",\"avatar_id\":%d}",
                        userId, avatarIds.get(i)));
            }
        }
        sb.append("]");
        String body = sb.toString();

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Recorded {} purchases: user={}, avatarIds={}, stripeSessionId={}",
                        avatarIds.size(), userId, avatarIds, stripeSessionId);
                return true;
            }
            log.warn("Failed to record purchases: status={}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.warn("Failed to record purchases: user={}, avatarIds={}", userId, avatarIds, e);
            return false;
        }
    }
}
