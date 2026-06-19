package io.github.oidcclient.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class RedisAuthorizationRequestStore implements AuthorizationRequestStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration timeToLive;
    private final String keyPrefix;

    public RedisAuthorizationRequestStore(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            Duration timeToLive,
            String keyPrefix
    ) {
        this.redis = Objects.requireNonNull(redis, "redis is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        if (timeToLive == null || timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
        this.timeToLive = timeToLive;
        this.keyPrefix = requireText(keyPrefix, "keyPrefix");
    }

    @Override
    public void save(AuthorizationRequest request) {
        Objects.requireNonNull(request, "request is required");
        // Redis TTL 和 state 一起保证授权请求短生命周期，过期后 callback 会被拒绝。
        redis.opsForValue().set(key(request.state()), write(StoredAuthorizationRequest.from(request)), timeToLive);
    }

    @Override
    public Optional<AuthorizationRequest> remove(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        // getAndDelete 保证 state 一次性消费，避免同一个授权码回调被重复处理。
        String value = redis.opsForValue().getAndDelete(key(state));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(read(value, StoredAuthorizationRequest.class).toAuthorizationRequest());
    }

    private String key(String state) {
        return keyPrefix + ":authorization-request:" + state;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new OAuthOidcClientException("failed to serialize authorization request", ex);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new OAuthOidcClientException("failed to deserialize authorization request", ex);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private record StoredAuthorizationRequest(
            String authorizationUri,
            String redirectUri,
            String state,
            String codeVerifier,
            String codeChallenge,
            String originalOrigin,
            String originalPath,
            String initPageUri
    ) {
        // 使用专门的存储 DTO，避免将来领域对象增加行为时影响 Redis 序列化格式。
        private static StoredAuthorizationRequest from(AuthorizationRequest request) {
            return new StoredAuthorizationRequest(
                    request.authorizationUri().toString(),
                    request.redirectUri().toString(),
                    request.state(),
                    request.codeVerifier(),
                    request.codeChallenge(),
                    request.originalOrigin().toString(),
                    request.originalPath(),
                    request.initPageUri().toString()
            );
        }

        private AuthorizationRequest toAuthorizationRequest() {
            return new AuthorizationRequest(
                    URI.create(authorizationUri),
                    URI.create(redirectUri),
                    state,
                    codeVerifier,
                    codeChallenge,
                    URI.create(originalOrigin),
                    originalPath,
                    URI.create(initPageUri)
            );
        }
    }
}
