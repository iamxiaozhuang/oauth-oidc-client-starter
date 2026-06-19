package io.github.oidcclient.client;

import java.util.Map;
import java.time.Instant;

public final class TokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final String refreshToken;
    private final String idToken;
    private final String scope;
    private final Instant expiresAt;
    private final Map<String, Object> raw;

    public TokenResponse(
            String accessToken,
            String tokenType,
            String refreshToken,
            String idToken,
            String scope,
            Instant expiresAt,
            Map<String, Object> raw
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.raw = raw;
    }

    public String accessToken() {
        return accessToken;
    }

    public String tokenType() {
        return tokenType;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public String idToken() {
        return idToken;
    }

    public String scope() {
        return scope;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Map<String, Object> raw() {
        return raw;
    }

    public boolean isExpired() {
        return expiresAt != null && !Instant.now().isBefore(expiresAt);
    }

    public boolean expiresWithin(java.time.Duration duration) {
        return expiresAt != null && !Instant.now().plus(duration).isBefore(expiresAt);
    }
}
