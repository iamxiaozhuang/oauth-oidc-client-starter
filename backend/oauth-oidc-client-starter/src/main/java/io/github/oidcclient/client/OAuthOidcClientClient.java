package io.github.oidcclient.client;

import io.github.oidcclient.client.internal.FormCodec;
import io.github.oidcclient.client.internal.Json;
import io.github.oidcclient.client.internal.Pkce;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class OAuthOidcClientClient implements AuthAdapter {
    private final OAuthOidcClientConfig config;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public OAuthOidcClientClient(OAuthOidcClientConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), Duration.ofSeconds(10));
    }

    public OAuthOidcClientClient(OAuthOidcClientConfig config, HttpClient httpClient) {
        this(config, httpClient, Duration.ofSeconds(10));
    }

    public OAuthOidcClientClient(OAuthOidcClientConfig config, HttpClient httpClient, Duration requestTimeout) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout is required");
    }

    @Override
    public AuthorizationRequest createAuthorizationRequest(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri) {
        Objects.requireNonNull(redirectUri, "redirectUri is required");
        Objects.requireNonNull(originalOrigin, "originalOrigin is required");
        Objects.requireNonNull(initPageUri, "initPageUri is required");
        if (originalPath == null || originalPath.isBlank()) {
            throw new IllegalArgumentException("originalPath is required");
        }
        // state 绑定本次登录请求，nonce 绑定 OIDC id_token，codeVerifier 留在后端。
        String state = Pkce.randomUrlSafe(24);
        String nonce = Pkce.randomUrlSafe(24);
        String codeVerifier = Pkce.randomUrlSafe(64);
        String codeChallenge = Pkce.challengeS256(codeVerifier);

        // 使用标准 Authorization Code + PKCE 参数，额外 provider 参数只在未占用标准字段时补充。
        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", config.clientId());
        query.put("redirect_uri", redirectUri.toString());
        query.put("scope", String.join(" ", config.scopes()));
        query.put("state", state);
        query.put("nonce", nonce);
        query.put("code_challenge", codeChallenge);
        query.put("code_challenge_method", "S256");
        config.authorizationParameters().forEach(query::putIfAbsent);

        URI authorizationUri = FormCodec.appendQuery(config.authorizationEndpoint(), query);
        return new AuthorizationRequest(
                authorizationUri,
                redirectUri,
                state,
                nonce,
                codeVerifier,
                codeChallenge,
                originalOrigin,
                originalPath,
                initPageUri
        );
    }

    @Override
    public TokenResponse exchangeCode(String code, String codeVerifier, URI redirectUri) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("codeVerifier is required");
        }
        Objects.requireNonNull(redirectUri, "redirectUri is required");

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", redirectUri.toString());
        form.put("client_id", config.clientId());
        form.put("code_verifier", codeVerifier);

        HttpRequest request = tokenRequestBuilder()
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(FormCodec.encode(form)))
                .build();

        Map<String, Object> payload = sendJson(request, "token exchange failed");
        return toTokenResponse(payload);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", config.clientId());

        HttpRequest request = tokenRequestBuilder()
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(FormCodec.encode(form)))
                .build();

        Map<String, Object> payload = sendJson(request, "token refresh failed");
        // 部分 provider 刷新时不会返回新的 refresh_token，此时继续沿用原值。
        if (!payload.containsKey("refresh_token")) {
            payload = new LinkedHashMap<>(payload);
            payload.put("refresh_token", refreshToken);
        }
        return toTokenResponse(payload);
    }

    private Map<String, Object> sendJson(HttpRequest request, String failureMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // 保留 provider 响应体，方便接入方排查 invalid_grant、redirect_uri mismatch 等配置问题。
                throw new OAuthOidcClientException(failureMessage + ": HTTP " + response.statusCode() + " " + response.body());
            }
            return Json.parseObject(response.body());
        } catch (IOException e) {
            throw new OAuthOidcClientException(failureMessage, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthOidcClientException(failureMessage, e);
        }
    }

    private HttpRequest.Builder tokenRequestBuilder() {
        HttpRequest.Builder builder = HttpRequest.newBuilder(config.tokenEndpoint())
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded");
        if (config.clientSecret() != null && !config.clientSecret().isBlank()) {
            builder.header("Authorization", basicAuthorization(config.clientId(), config.clientSecret()));
        }
        return builder;
    }

    private static String basicAuthorization(String clientId, String clientSecret) {
        String credentials = urlEncode(clientId) + ":" + urlEncode(clientSecret);
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static TokenResponse toTokenResponse(Map<String, Object> payload) {
        String accessToken = stringValue(payload.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new OAuthOidcClientException("token response does not contain access_token");
        }

        // OAuth2 返回的是 expires_in 秒数，运行时统一转换成绝对过期时间，便于提前刷新。
        Instant expiresAt = null;
        Long expiresIn = longValue(payload.get("expires_in"));
        if (expiresIn != null) {
            expiresAt = Instant.now().plusSeconds(expiresIn);
        }

        return new TokenResponse(
                accessToken,
                stringValue(payload.getOrDefault("token_type", "Bearer")),
                stringValue(payload.get("refresh_token")),
                stringValue(payload.get("id_token")),
                stringValue(payload.get("scope")),
                expiresAt,
                Map.copyOf(payload)
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
