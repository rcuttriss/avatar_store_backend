package com.avatarstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final RestTemplate restTemplate;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:avatars}")
    private String defaultBucket;

    /**
     * Download a file from Supabase Storage (authenticated).
     *
     * @param bucket bucket name (null to use default)
     * @param path   object path within the bucket
     * @return file bytes, or null if not found or error
     */
    public byte[] downloadFile(String bucket, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String bucketName = bucket != null && !bucket.isBlank() ? bucket : defaultBucket;
        String url = buildDownloadUrl(bucketName, path);
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("Failed to download from Supabase Storage: bucket={}, path={}", bucketName, path, e);
            return null;
        }
    }

    /**
     * Download using the default bucket.
     */
    public byte[] downloadFile(String path) {
        return downloadFile(null, path);
    }

    /**
     * Upload a file to Supabase Storage (authenticated).
     *
     * @param file   the file to upload
     * @param bucket bucket name (null to use default)
     * @param path   object path (null to generate a unique path from original filename)
     * @return the path under the bucket, or null on failure
     */
    public String uploadFile(MultipartFile file, String bucket, String path) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String bucketName = bucket != null && !bucket.isBlank() ? bucket : defaultBucket;
        if (path == null || path.isBlank()) {
            String name = file.getOriginalFilename();
            String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf(".")) : "";
            path = UUID.randomUUID().toString() + ext;
        }
        String url = buildUploadUrl(bucketName, path);
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        try {
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Uploaded to Supabase Storage: bucket={}, path={}", bucketName, path);
                return path;
            }
        } catch (Exception e) {
            log.warn("Failed to upload to Supabase Storage: bucket={}, path={}", bucketName, path, e);
        }
        return null;
    }

    /**
     * Upload using the default bucket; path is auto-generated if not provided.
     */
    public String uploadFile(MultipartFile file, String path) {
        return uploadFile(file, null, path);
    }

    private String buildUploadUrl(String bucket, String path) {
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedPath = path.replace(" ", "%20");
        return base + "/storage/v1/object/" + bucket + "/" + encodedPath;
    }

    private String buildDownloadUrl(String bucket, String path) {
        String base = supabaseUrl != null ? supabaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedPath = path.replace(" ", "%20");
        return base + "/storage/v1/object/authenticated/" + bucket + "/" + encodedPath;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
