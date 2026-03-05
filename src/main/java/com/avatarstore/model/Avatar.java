package com.avatarstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Avatar {
    private Long id;
    private String name;
    private String slug;
    
    @JsonProperty("poster_url")
    private String posterUrl;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    
    private String description;
    
    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("is_featured")
    private Boolean isFeatured;
    
    private String category;
    private String platform;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}

