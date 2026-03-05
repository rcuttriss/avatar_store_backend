package com.avatarstore.controller;

import com.avatarstore.config.SupabaseJwtHelper;
import com.avatarstore.dto.ApiResponse;
import com.avatarstore.model.Avatar;
import com.avatarstore.model.AvatarVersion;
import com.avatarstore.service.AvatarService;
import com.avatarstore.service.PurchaseService;
import com.avatarstore.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final SupabaseStorageService supabaseStorageService;
    private final SupabaseJwtHelper supabaseJwtHelper;
    private final AvatarService avatarService;
    private final PurchaseService purchaseService;

    /**
     * Download avatar file. Requires Authorization: Bearer &lt;access_token&gt;.
     * Only users who have purchased this avatar may download. Use slug or avatarId.
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("versionId") Long versionId) {

        Optional<UUID> userIdOpt = supabaseJwtHelper.getUserIdFromAuthorization(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!purchaseService.hasPurchased(userIdOpt.get(), versionId)) {
            log.debug("Download forbidden: no purchase found for user={}, versionId={}", userIdOpt.get(), versionId);
            return ResponseEntity.status(403).build();
        }

        AvatarVersion version;
        try {
            version = avatarService.getVersionById(versionId);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }

        byte[] bytes = supabaseStorageService.downloadFile(version.getBlobContainerName(), version.getBlobFilePath());
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        String filename = version.getBlobFileName() != null ? version.getBlobFileName() : ("avatar-version-" + versionId + ".vrca");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /**
     * Upload a file. POST multipart/form-data with "file"; optional "path" for object path.
     * Returns the stored path on success.
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {
        if (file.isEmpty()) {
            return ApiResponse.error("File is empty");
        }
        String storedPath = supabaseStorageService.uploadFile(file, path);
        if (storedPath == null) {
            return ApiResponse.error("Upload failed");
        }
        return ApiResponse.success(Map.of("path", storedPath), 1);
    }
}
