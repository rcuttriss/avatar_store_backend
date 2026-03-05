package com.avatarstore.service;

import com.avatarstore.dto.PurchasedItem;
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
    public boolean hasPurchased(UUID userId, Long versionId) {
        if (userId == null || versionId == null) return false;
        String base = baseUrl();
        String url = base + "/rest/v1/purchases?user_id=eq." + userId + "&avatar_version_id=eq." + versionId + "&select=id&limit=1";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().trim().isEmpty()) return false;
            List<Map<String, Object>> rows = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            return rows != null && !rows.isEmpty();
        } catch (Exception e) {
            log.warn("Failed to check purchase: user={}, versionId={}", userId, versionId, e);
            return false;
        }
    }

    /**
     * Bulk-inserts purchase records for multiple versions in a single Supabase call.
     */
    public boolean recordPurchases(UUID userId, List<Long> avatarIds, List<Long> versionIds, String stripeSessionId) {
        if (userId == null || versionIds == null || versionIds.isEmpty()) return false;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < versionIds.size(); i++) {
            if (i > 0) sb.append(",");
            if (stripeSessionId != null && !stripeSessionId.isBlank()) {
                sb.append(String.format(
                        "{\"user_id\":\"%s\",\"avatar_id\":%d,\"avatar_version_id\":%d,\"stripe_session_id\":\"%s\"}",
                        userId, avatarIds.get(i), versionIds.get(i), stripeSessionId));
            } else {
                sb.append(String.format(
                        "{\"user_id\":\"%s\",\"avatar_id\":%d,\"avatar_version_id\":%d}",
                        userId, avatarIds.get(i), versionIds.get(i)));
            }
        }
        sb.append("]");

        HttpHeaders headers = createHeaders();
        headers.set("Prefer", "return=minimal");
        HttpEntity<String> entity = new HttpEntity<>(sb.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl() + "/rest/v1/purchases", HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Recorded {} purchases: user={}, versionIds={}", versionIds.size(), userId, versionIds);
                return true;
            }
            log.warn("Failed to record purchases: status={}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.warn("Failed to record purchases: user={}, versionIds={}", userId, versionIds, e);
            return false;
        }
    }

    /**
     * Returns all purchases for a user with avatar and version details via PostgREST join.
     */
    public List<PurchasedItem> getPurchases(UUID userId) {
        if (userId == null) return List.of();
        String url = baseUrl()
                + "/rest/v1/purchases"
                + "?select=id,created_at,avatar_id,avatar_version_id,stripe_session_id"
                + ",avatars(id,name,slug,thumbnail_url,poster_url)"
                + ",avatar_versions(id,name,price,description,blob_container_name,blob_file_path,blob_file_name)"
                + "&user_id=eq." + userId
                + "&order=created_at.desc";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().trim().isEmpty()) return List.of();
            return objectMapper.readValue(response.getBody(), new TypeReference<List<PurchasedItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch purchases for user={}", userId, e);
            return List.of();
        }
    }

    private String baseUrl() {
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
