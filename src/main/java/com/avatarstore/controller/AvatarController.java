package com.avatarstore.controller;

import com.avatarstore.dto.ApiResponse;
import com.avatarstore.model.Avatar;
import com.avatarstore.service.SupabaseService;
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
    
    private final SupabaseService supabaseService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Avatar>>> getAvatars(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String slug) {
        
        try {
            // Handle query parameters for backward compatibility
            if (id != null) {
                return getAvatarById(id);
            }
            
            if (slug != null) {
                return getAvatarBySlug(slug);
            }
            
            // Return all avatars
            List<Avatar> avatars = supabaseService.getAllAvatars();
            return ResponseEntity.ok(ApiResponse.success(avatars, avatars.size()));
        } catch (Exception error) {
            log.error("Error fetching avatars", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatars"));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<List<Avatar>>> getAvatarById(@PathVariable Long id) {
        try {
            Avatar avatar = supabaseService.getAvatarById(id);
            List<Avatar> avatarList = List.of(avatar);
            return ResponseEntity.ok(ApiResponse.success(avatarList, 1));
        } catch (RuntimeException error) {
            log.error("Error fetching avatar by id: {}", id, error);
            if (error.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<List<Avatar>>error("Avatar not found"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatar"));
        } catch (Exception error) {
            log.error("Error fetching avatar by id: {}", id, error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Avatar>>error("Failed to fetch avatar"));
        }
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<List<Avatar>>> getAvatarBySlug(@PathVariable String slug) {
        try {
            Avatar avatar = supabaseService.getAvatarBySlug(slug);
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

