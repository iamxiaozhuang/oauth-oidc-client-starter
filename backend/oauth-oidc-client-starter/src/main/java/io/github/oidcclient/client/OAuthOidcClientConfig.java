package io.github.oidcclient.client;

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.Objects;

public final class OAuthOidcClientConfig {
    private final URI authorizationEndpoint;
    private final URI tokenEndpoint;
    private final URI userInfoEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final URI redirectUri;
    private final List<String> scopes;
    private final Map<String, String> authorizationParameters;

    private OAuthOidcClientConfig(Builder builder) {
        this.authorizationEndpoint = require(builder.authorizationEndpoint, "authorizationEndpoint");
        this.tokenEndpoint = require(builder.tokenEndpoint, "tokenEndpoint");
        this.userInfoEndpoint = require(builder.userInfoEndpoint, "userInfoEndpoint");
        this.clientId = requireText(builder.clientId, "clientId");
        this.clientSecret = builder.clientSecret;
        this.redirectUri = builder.redirectUri;
        this.scopes = List.copyOf(builder.scopes);
        this.authorizationParameters = Map.copyOf(builder.authorizationParameters);
    }

    public URI authorizationEndpoint() {
        return authorizationEndpoint;
    }

    public URI tokenEndpoint() {
        return tokenEndpoint;
    }

    public URI userInfoEndpoint() {
        return userInfoEndpoint;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public URI redirectUri() {
        return redirectUri;
    }

    public List<String> scopes() {
        return scopes;
    }

    public Map<String, String> authorizationParameters() {
        return authorizationParameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static <T> T require(T value, String field) {
        return Objects.requireNonNull(value, field + " is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public static final class Builder {
        private URI authorizationEndpoint;
        private URI tokenEndpoint;
        private URI userInfoEndpoint;
        private String clientId;
        private String clientSecret;
        private URI redirectUri;
        private List<String> scopes = List.of("openid", "profile", "email");
        private Map<String, String> authorizationParameters = Map.of();

        public Builder authorizationEndpoint(URI authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
            return this;
        }

        public Builder tokenEndpoint(URI tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
            return this;
        }

        public Builder userInfoEndpoint(URI userInfoEndpoint) {
            this.userInfoEndpoint = userInfoEndpoint;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder redirectUri(URI redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder scopes(List<String> scopes) {
            this.scopes = List.copyOf(scopes);
            return this;
        }

        public Builder authorizationParameters(Map<String, String> authorizationParameters) {
            this.authorizationParameters = Map.copyOf(authorizationParameters);
            return this;
        }

        public OAuthOidcClientConfig build() {
            return new OAuthOidcClientConfig(this);
        }
    }
}
