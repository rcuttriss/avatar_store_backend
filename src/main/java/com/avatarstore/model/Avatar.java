package com.avatarstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    
    private BigDecimal price;
    private String description;
    
    @JsonProperty("short_description")
    private String shortDescription;
    
    @JsonProperty("poly_count")
    private Integer polyCount;
    
    @JsonProperty("mat_count")
    private Integer matCount;
    
    @JsonProperty("mesh_count")
    private Integer meshCount;
    
    @JsonProperty("texture_memory")
    private String textureMemory;
    
    @JsonProperty("download_size")
    private String downloadSize;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("is_featured")
    private Boolean isFeatured;
    
    private String category;
    private String platform;
    
    @JsonProperty("blob_container_name")
    private String blobContainerName;
    
    @JsonProperty("blob_file_path")
    private String blobFilePath;
    
    @JsonProperty("blob_file_name")
    private String blobFileName;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}

