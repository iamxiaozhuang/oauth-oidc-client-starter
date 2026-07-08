package io.github.oidcclient.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RedisBffSessionStore implements BffSessionStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration timeToLive;
    private final String keyPrefix;

    public RedisBffSessionStore(
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
    public void save(BffSession session) {
        Objects.requireNonNull(session, "session is required");
        // 服务端 session 存放 token 和 id_token 派生的用户信息，浏览器 cookie 只保存随机 session id。
        redis.opsForValue().set(key(session.id()), write(StoredBffSession.from(session)), timeToLive);
    }

    @Override
    public Optional<BffSession> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().get(key(sessionId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(read(value, StoredBffSession.class).toBffSession());
    }

    @Override
    public void delete(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            // 注销时删除服务端 session，cookie 清理由 Web 层负责。
            redis.delete(key(sessionId));
        }
    }

    private String key(String sessionId) {
        return keyPrefix + ":session:" + sessionId;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new OAuthOidcClientException("failed to serialize BFF session", ex);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new OAuthOidcClientException("failed to deserialize BFF session", ex);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private record StoredBffSession(
            String id,
            StoredTokenResponse token,
            StoredUserInfo userInfo,
            Instant createdAt,
            Instant updatedAt
    ) {
        // Redis 中显式展开 token 和用户信息字段，便于长期兼容和排查。
        private static StoredBffSession from(BffSession session) {
            return new StoredBffSession(
                    session.id(),
                    StoredTokenResponse.from(session.token()),
                    StoredUserInfo.from(session.userInfo()),
                    session.createdAt(),
                    session.updatedAt()
            );
        }

        private BffSession toBffSession() {
            return new BffSession(id, token.toTokenResponse(), userInfo.toUserInfo(), createdAt, updatedAt);
        }
    }

    private record StoredTokenResponse(
            String accessToken,
            String tokenType,
            String refreshToken,
            String idToken,
            String scope,
            Instant expiresAt,
            Map<String, Object> raw
    ) {
        private static StoredTokenResponse from(TokenResponse token) {
            return new StoredTokenResponse(
                    token.accessToken(),
                    token.tokenType(),
                    token.refreshToken(),
                    token.idToken(),
                    token.scope(),
                    token.expiresAt(),
                    token.raw()
            );
        }

        private TokenResponse toTokenResponse() {
            return new TokenResponse(accessToken, tokenType, refreshToken, idToken, scope, expiresAt, raw);
        }
    }

    private record StoredUserInfo(
            String subject,
            String name,
            String email,
            Map<String, Object> claims
    ) {
        private static StoredUserInfo from(UserInfo userInfo) {
            return new StoredUserInfo(userInfo.subject(), userInfo.name(), userInfo.email(), userInfo.claims());
        }

        private UserInfo toUserInfo() {
            return new UserInfo(subject, name, email, claims);
        }
    }
}
