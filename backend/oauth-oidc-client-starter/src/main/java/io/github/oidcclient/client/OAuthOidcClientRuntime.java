package io.github.oidcclient.client;

import io.github.oidcclient.client.internal.Pkce;
import io.github.oidcclient.client.internal.FormCodec;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class OAuthOidcClientRuntime {
    private final AuthAdapter authAdapter;
    private final AuthorizationRequestStore authorizationRequests;
    private final BffSessionStore sessions;
    private final Duration refreshSkew;
    // 以 sessionId 为粒度控制刷新并发，避免同一个 refresh token 被多个请求同时使用。
    private final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public OAuthOidcClientRuntime(
            AuthAdapter authAdapter,
            AuthorizationRequestStore authorizationRequests,
            BffSessionStore sessions,
            Duration refreshSkew
    ) {
        this.authAdapter = Objects.requireNonNull(authAdapter, "authAdapter is required");
        this.authorizationRequests = Objects.requireNonNull(authorizationRequests, "authorizationRequests is required");
        this.sessions = Objects.requireNonNull(sessions, "sessions is required");
        this.refreshSkew = Objects.requireNonNull(refreshSkew, "refreshSkew is required");
    }

    public URI beginLogin(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri) {
        // 这里创建并保存一次性授权请求，state 用于防 CSRF，codeVerifier 用于 PKCE 换 token。
        AuthorizationRequest request = authAdapter.createAuthorizationRequest(redirectUri, originalOrigin, originalPath, initPageUri);
        authorizationRequests.save(request);
        return request.authorizationUri();
    }

    public OAuthOidcClientCallbackResult completeCallback(String code, String state, URI redirectUri, URI currentOrigin, String targetParam) {
        // remove 而不是 find：同一个 state 只能消费一次，降低重放攻击风险。
        AuthorizationRequest request = authorizationRequests.remove(state)
                .orElseThrow(() -> new OAuthOidcClientException("authorization state is missing or expired"));
        validateCallbackContext(request, redirectUri, currentOrigin);

        // code_verifier 从后端取出并发送到 token endpoint，浏览器全程不可见。
        TokenResponse token = authAdapter.exchangeCode(code, request.codeVerifier(), request.redirectUri());
        UserInfo userInfo = authAdapter.fetchUserInfo(token.accessToken());
        BffSession session = new BffSession(
                Pkce.randomUrlSafe(32),
                token,
                userInfo,
                Instant.now(),
                Instant.now()
        );
        sessions.save(session);
        return new OAuthOidcClientCallbackResult(session, initRedirectUri(request, targetParam));
    }

    public String currentUserId(String sessionId) {
        return currentSession(sessionId).currentUserId();
    }

    public UserInfo currentUser(String sessionId) {
        return currentSession(sessionId).userInfo();
    }

    public TokenResponse currentToken(String sessionId) {
        BffSession session = currentSession(sessionId);
        TokenResponse token = session.token();
        // 未进入刷新窗口时直接返回现有 token，减少对授权服务器的请求。
        if (!shouldRefresh(token)) {
            return token;
        }
        // 没有 refresh token 时，未真正过期的 token 还能使用；已过期则不能继续透传给下游。
        if (token.refreshToken() == null || token.refreshToken().isBlank()) {
            if (token.isExpired()) {
                throw new OAuthOidcClientException("access token is expired and refresh token is missing");
            }
            return token;
        }

        Object lock = refreshLocks.computeIfAbsent(sessionId, ignored -> new Object());
        synchronized (lock) {
            try {
                // 加锁后再次读取 session，防止等待锁期间其他线程已经完成刷新。
                BffSession latest = currentSession(sessionId);
                if (!shouldRefresh(latest.token())) {
                    return latest.token();
                }
                TokenResponse refreshed = authAdapter.refreshToken(latest.token().refreshToken());
                BffSession refreshedSession = latest.withToken(refreshed);
                sessions.save(refreshedSession);
                return refreshed;
            } finally {
                refreshLocks.remove(sessionId, lock);
            }
        }
    }

    public void logout(String sessionId) {
        sessions.delete(sessionId);
    }

    private BffSession currentSession(String sessionId) {
        return sessions.find(sessionId)
                .orElseThrow(() -> new OAuthOidcClientException("BFF session is missing or expired"));
    }

    private static void validateCallbackContext(AuthorizationRequest request, URI redirectUri, URI currentOrigin) {
        if (!request.redirectUri().equals(redirectUri)) {
            throw new OAuthOidcClientException("callback redirect_uri does not match authorization request");
        }
        if (!request.originalOrigin().equals(currentOrigin)) {
            throw new OAuthOidcClientException("callback origin does not match authorization request");
        }
    }

    private static URI initRedirectUri(AuthorizationRequest request, String targetParam) {
        return FormCodec.appendQuery(request.initPageUri(), Map.of(targetParam, request.originalPath()));
    }

    private boolean shouldRefresh(TokenResponse token) {
        Instant expiresAt = token.expiresAt();
        // refreshSkew 提前量用于避开“刚返回前端就过期”的临界窗口。
        return expiresAt != null && !Instant.now().plus(refreshSkew).isBefore(expiresAt);
    }
}
