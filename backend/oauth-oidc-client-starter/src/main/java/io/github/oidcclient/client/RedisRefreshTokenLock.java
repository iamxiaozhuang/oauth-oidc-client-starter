package io.github.oidcclient.client;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public final class RedisRefreshTokenLock implements RefreshTokenLock {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final Duration timeToLive;
    private final Duration waitTimeout;
    private final Duration retryInterval;

    public RedisRefreshTokenLock(
            StringRedisTemplate redis,
            String keyPrefix,
            Duration timeToLive,
            Duration waitTimeout,
            Duration retryInterval
    ) {
        this.redis = Objects.requireNonNull(redis, "redis is required");
        this.keyPrefix = requireText(keyPrefix, "keyPrefix");
        this.timeToLive = requirePositive(timeToLive, "timeToLive");
        this.waitTimeout = requirePositive(waitTimeout, "waitTimeout");
        this.retryInterval = requirePositive(retryInterval, "retryInterval");
    }

    @Override
    public LockHandle acquire(String sessionId) {
        String key = key(sessionId);
        String owner = UUID.randomUUID().toString();
        Instant deadline = Instant.now().plus(waitTimeout);
        while (true) {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, owner, timeToLive);
            if (Boolean.TRUE.equals(acquired)) {
                return () -> release(key, owner);
            }
            if (!Instant.now().isBefore(deadline)) {
                throw new OAuthOidcClientException("timed out waiting for refresh token lock");
            }
            sleep();
        }
    }

    private void release(String key, String owner) {
        redis.execute(RELEASE_SCRIPT, Collections.singletonList(key), owner);
    }

    private void sleep() {
        try {
            Thread.sleep(Math.max(1, retryInterval.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OAuthOidcClientException("interrupted while waiting for refresh token lock", ex);
        }
    }

    private String key(String sessionId) {
        return keyPrefix + ":refresh-lock:" + requireText(sessionId, "sessionId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String field) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
