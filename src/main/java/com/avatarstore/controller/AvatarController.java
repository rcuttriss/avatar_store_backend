package com.avatarstore.controller;

import com.avatarstore.dto.ApiResponse;
import com.avatarstore.model.Avatar;
import com.avatarstore.model.AvatarVersion;
import com.avatarstore.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avatars")
@RequiredArgsConstructor
@Slf4j
public class AvatarController {
    
    private final AvatarService avatarService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Avatar>>> getAvatars() {
        try {
            // Return all avatars
            List<Avatar> avatars = avatarService.getAllAvatars();
            return ResponseEntity.ok(ApiResponse.success(avatars, avatars.size()));
        } catch (Exception error) {
            log.error("Error fetching avatars", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatars"));
        }
    }
    
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<AvatarVersion>>> getVersionsByAvatarId(@PathVariable Long id) {
        try {
            List<AvatarVersion> versions = avatarService.getVersionsByAvatarId(id);
            return ResponseEntity.ok(ApiResponse.success(versions, versions.size()));
        } catch (Exception error) {
            log.error("Error fetching versions for avatar id: {}", id, error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<AvatarVersion>>error("Failed to fetch avatar versions"));
        }
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<List<Avatar>>> getAvatarBySlug(@PathVariable String slug) {
        try {
            Avatar avatar = avatarService.getAvatarBySlug(slug);
            List<Avatar> avatarList = List.of(avatar);
            return ResponseEntity.ok(ApiResponse.success(avatarList, 1));
        } catch (RuntimeException error) {
            log.error("Error fetching avatar by slug: {}", slug, error);
            if (error.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<List<Avatar>>error("Avatar not found"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatar"));
        } catch (Exception error) {
            log.error("Error fetching avatar by slug: {}", slug, error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatar"));
        }
    }
}

