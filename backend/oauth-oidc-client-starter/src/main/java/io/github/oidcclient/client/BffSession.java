package io.github.oidcclient.client;

import java.time.Instant;
import java.util.Objects;

public final class BffSession {
    // 随机 session id 暴露给浏览器 cookie；token 和 userInfo 只存放在后端。
    private final String id;
    private final TokenResponse token;
    private final UserInfo userInfo;
    private final Instant createdAt;
    private final Instant updatedAt;

    public BffSession(String id, TokenResponse token, UserInfo userInfo, Instant createdAt, Instant updatedAt) {
        this.id = requireText(id, "id");
        this.token = Objects.requireNonNull(token, "token is required");
        this.userInfo = Objects.requireNonNull(userInfo, "userInfo is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public String id() {
        return id;
    }

    public TokenResponse token() {
        return token;
    }

    public UserInfo userInfo() {
        return userInfo;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String currentUserId() {
        return userInfo.subject();
    }

    public BffSession withToken(TokenResponse nextToken) {
        // 刷新 token 时保留同一个 session id 和用户信息，只更新时间戳与 token。
        return new BffSession(id, nextToken, userInfo, createdAt, Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
