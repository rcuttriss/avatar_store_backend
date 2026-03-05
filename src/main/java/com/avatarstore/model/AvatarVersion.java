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
public class AvatarVersion {
    private Long id;

    @JsonProperty("avatar_id")
    private Long avatarId;

    private String name;
    private BigDecimal price;
    private String description;

    @JsonProperty("blob_container_name")
    private String blobContainerName;

    @JsonProperty("blob_file_path")
    private String blobFilePath;

    @JsonProperty("blob_file_name")
    private String blobFileName;

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

    @JsonProperty("is_default")
    private Boolean isDefault;

    @JsonProperty("sort_order")
    private Integer sortOrder;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
