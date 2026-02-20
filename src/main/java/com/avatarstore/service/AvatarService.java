package com.avatarstore.service;

import com.avatarstore.model.Avatar;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${supabase.url:}")
    private String supabaseUrl;
    
    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;
    
    private void validateConfiguration() {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Supabase URL is not configured. Please set supabase.url in application.properties or SUPABASE_URL environment variable.");
        }
        if (serviceRoleKey == null || serviceRoleKey.trim().isEmpty()) {
            throw new IllegalStateException("Supabase service role key is not configured. Please set supabase.service-role-key in application.properties or SUPABASE_SERVICE_ROLE_KEY environment variable.");
        }
        // Ensure URL doesn't end with / to avoid double slashes
        supabaseUrl = supabaseUrl.trim();
        if (supabaseUrl.endsWith("/")) {
            supabaseUrl = supabaseUrl.substring(0, supabaseUrl.length() - 1);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }
    
    private String buildUrl(String path) {
        validateConfiguration();
        return supabaseUrl + path;
    }
    
    public List<Avatar> getAllAvatars() {
        try {
            String url = buildUrl("/rest/v1/avatars?order=created_at.desc");
            log.debug("Fetching avatars from URL: {}", url);
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getBody() == null || response.getBody().trim().isEmpty()) {
                log.warn("Received empty response from Supabase");
                return List.of();
            }
            
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Avatar>>() {});
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching avatars from Supabase. URL: {}, Error: {}", supabaseUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch avatars: " + e.getMessage(), e);
        }
    }
    
    public Avatar getAvatarById(Long id) {
        try {
            String url = buildUrl("/rest/v1/avatars?id=eq." + id);
            log.debug("Fetching avatar by id from URL: {}", url);
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getBody() == null || response.getBody().trim().isEmpty()) {
                throw new RuntimeException("Avatar not found");
            }
            
            List<Avatar> avatars = objectMapper.readValue(response.getBody(), new TypeReference<List<Avatar>>() {});
            if (avatars.isEmpty()) {
                throw new RuntimeException("Avatar not found");
            }
            return avatars.get(0);
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching avatar by id from Supabase. URL: {}, Error: {}", supabaseUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch avatar: " + e.getMessage(), e);
        }
    }
    
    public Avatar getAvatarBySlug(String slug) {
        try {
            String encodedSlug = java.net.URLEncoder.encode(slug, java.nio.charset.StandardCharsets.UTF_8);
            String url = buildUrl("/rest/v1/avatars?slug=eq." + encodedSlug);
            log.debug("Fetching avatar by slug from URL: {}", url);
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getBody() == null || response.getBody().trim().isEmpty()) {
                throw new RuntimeException("Avatar not found");
            }
            
            List<Avatar> avatars = objectMapper.readValue(response.getBody(), new TypeReference<List<Avatar>>() {});
            if (avatars.isEmpty()) {
                throw new RuntimeException("Avatar not found");
            }
            return avatars.get(0);
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching avatar by slug from Supabase. URL: {}, Error: {}", supabaseUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch avatar: " + e.getMessage(), e);
        }
    }
}

