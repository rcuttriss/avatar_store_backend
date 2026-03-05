package com.avatarstore.dto;

import com.avatarstore.model.Avatar;
import com.avatarstore.model.AvatarVersion;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record PurchasedItem(
        Long id,
        @JsonProperty("avatar_id") Long avatarId,
        @JsonProperty("avatar_version_id") Long avatarVersionId,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("stripe_session_id") String stripeSessionId,
        @JsonProperty("avatars") Avatar avatar,
        @JsonProperty("avatar_versions") AvatarVersion version) {}
