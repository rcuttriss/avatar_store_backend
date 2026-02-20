package com.avatarstore.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifies Supabase Auth JWTs (HS256) and extracts the user id (sub claim).
 * Used to identify the caller for purchase-gated download.
 */
@Component
@Slf4j
public class SupabaseJwtHelper {

    @Value("${supabase.jwt-secret:}")
    private String jwtSecret;

    /**
     * Parses Authorization: Bearer &lt;token&gt; and returns the user id if the token is valid.
     * @param authorizationHeader value of the Authorization header (e.g. "Bearer eyJ...")
     * @return the user UUID from the "sub" claim, or empty if missing/invalid/expired
     */
    public Optional<UUID> getUserIdFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.debug("JWT missing or not Bearer: Authorization header absent or invalid");
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.debug("JWT empty after Bearer prefix");
            return Optional.empty();
        }
        log.debug("JWT received: {}... (length={})", token.substring(0, Math.min(30, token.length())), token.length());
        return getUserIdFromToken(token);
    }

    /**
     * Verifies the JWT (HS256) and returns the user id from the "sub" claim.
     *
     * @param token the raw JWT string
     * @return the user UUID, or empty if invalid or jwt-secret not configured
     */
    public Optional<UUID> getUserIdFromToken(String token) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.warn("supabase.jwt-secret is not configured; cannot verify JWT");
            return Optional.empty();
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            DecodedJWT decoded = JWT.require(algorithm)
                    .build()
                    .verify(token);
            String sub = decoded.getSubject();
            if (sub == null || sub.isBlank()) {
                log.debug("JWT verified but sub claim missing");
                return Optional.empty();
            }
            log.info("JWT verified: sub={}, iss={}, exp={}", sub, decoded.getIssuer(), decoded.getExpiresAt());
            return Optional.of(UUID.fromString(sub));
        } catch (JWTVerificationException e) {
            log.debug("Invalid or expired JWT: {}", e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.debug("Invalid sub claim: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
