package io.github.oidcclient.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthOidcClientRuntimeTest {
    @Test
    void currentTokenRefreshesExpiringAccessTokenAndSavesSession() {
        CapturingAuthAdapter authAdapter = new CapturingAuthAdapter();
        CapturingSessionStore sessions = new CapturingSessionStore(sessionWithToken(
                token("old-access-token", "refresh-token", Instant.now().plusSeconds(20))
        ));
        OAuthOidcClientRuntime runtime = new OAuthOidcClientRuntime(
                authAdapter,
                new CapturingAuthorizationRequestStore(),
                sessions,
                Duration.ofSeconds(60)
        );

        TokenResponse token = runtime.currentToken("session-1");

        assertThat(token.accessToken()).isEqualTo("new-access-token");
        assertThat(authAdapter.refreshedWith).isEqualTo("refresh-token");
        assertThat(sessions.saved.token().accessToken()).isEqualTo("new-access-token");
        assertThat(sessions.saved.token().refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void currentTokenRejectsExpiredAccessTokenWhenRefreshTokenIsMissing() {
        CapturingSessionStore sessions = new CapturingSessionStore(sessionWithToken(
                token("expired-access-token", null, Instant.now().minusSeconds(1))
        ));
        OAuthOidcClientRuntime runtime = new OAuthOidcClientRuntime(
                new CapturingAuthAdapter(),
                new CapturingAuthorizationRequestStore(),
                sessions,
                Duration.ofSeconds(60)
        );

        assertThatThrownBy(() -> runtime.currentToken("session-1"))
                .isInstanceOf(OAuthOidcClientException.class)
                .hasMessageContaining("refresh token is missing");
    }

    @Test
    void currentTokenUsesSharedRefreshLockAcrossRuntimeInstances() throws Exception {
        BlockingAuthAdapter authAdapter = new BlockingAuthAdapter();
        CapturingSessionStore sessions = new CapturingSessionStore(sessionWithToken(
                token("old-access-token", "refresh-token", Instant.now().plusSeconds(20))
        ));
        RefreshTokenLock refreshTokenLock = new InMemoryRefreshTokenLock();
        OAuthOidcClientRuntime firstRuntime = new OAuthOidcClientRuntime(
                authAdapter,
                new CapturingAuthorizationRequestStore(),
                sessions,
                Duration.ofSeconds(60),
                refreshTokenLock
        );
        OAuthOidcClientRuntime secondRuntime = new OAuthOidcClientRuntime(
                authAdapter,
                new CapturingAuthorizationRequestStore(),
                sessions,
                Duration.ofSeconds(60),
                refreshTokenLock
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<TokenResponse> firstCall = () -> firstRuntime.currentToken("session-1");
            Callable<TokenResponse> secondCall = () -> secondRuntime.currentToken("session-1");
            var first = executor.submit(firstCall);
            assertThat(authAdapter.refreshStarted.await(3, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(secondCall);
            authAdapter.continueRefresh.countDown();

            assertThat(first.get(3, TimeUnit.SECONDS).accessToken()).isEqualTo("new-access-token");
            assertThat(second.get(3, TimeUnit.SECONDS).accessToken()).isEqualTo("new-access-token");
            assertThat(authAdapter.refreshCount).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void logoutDeletesBffSession() {
        CapturingSessionStore sessions = new CapturingSessionStore(sessionWithToken(
                token("access-token", "refresh-token", Instant.now().plusSeconds(600))
        ));
        OAuthOidcClientRuntime runtime = new OAuthOidcClientRuntime(
                new CapturingAuthAdapter(),
                new CapturingAuthorizationRequestStore(),
                sessions,
                Duration.ofSeconds(60)
        );

        runtime.logout("session-1");

        assertThat(sessions.find("session-1")).isEmpty();
    }

    @Test
    void callbackRedirectsToOriginalOriginInitPageWithOriginalPathTarget() {
        CapturingAuthAdapter authAdapter = new CapturingAuthAdapter();
        CapturingAuthorizationRequestStore authorizationRequests = new CapturingAuthorizationRequestStore();
        OAuthOidcClientRuntime runtime = new OAuthOidcClientRuntime(
                authAdapter,
                authorizationRequests,
                new CapturingSessionStore(null),
                Duration.ofSeconds(60)
        );
        URI redirectUri = URI.create("https://app-a.example.com/oauth/callback");
        URI originalOrigin = URI.create("https://app-a.example.com");
        URI initPageUri = URI.create("https://app-a.example.com/login-page");

        runtime.beginLogin(redirectUri, originalOrigin, "/dashboard?tab=home", initPageUri);
        OAuthOidcClientCallbackResult result = runtime.completeCallback(
                "code",
                "state",
                redirectUri,
                originalOrigin,
                "target"
        );

        assertThat(result.initRedirectUri().toString())
                .isEqualTo("https://app-a.example.com/login-page?target=%2Fdashboard%3Ftab%3Dhome");
        assertThat(result.session().currentUserId()).isEqualTo("user-1");
    }

    @Test
    void callbackRejectsIdTokenWithMismatchedNonce() {
        CapturingAuthAdapter authAdapter = new CapturingAuthAdapter("wrong-nonce");
        CapturingAuthorizationRequestStore authorizationRequests = new CapturingAuthorizationRequestStore();
        CapturingSessionStore sessions = new CapturingSessionStore(null);
        OAuthOidcClientRuntime runtime = new OAuthOidcClientRuntime(
                authAdapter,
                authorizationRequests,
                sessions,
                Duration.ofSeconds(60)
        );
        URI redirectUri = URI.create("https://app-a.example.com/oauth/callback");
        URI originalOrigin = URI.create("https://app-a.example.com");

        runtime.beginLogin(
                redirectUri,
                originalOrigin,
                "/dashboard",
                URI.create("https://app-a.example.com/login-page")
        );

        assertThatThrownBy(() -> runtime.completeCallback("code", "state", redirectUri, originalOrigin, "target"))
                .isInstanceOf(OAuthOidcClientException.class)
                .hasMessageContaining("nonce");
        assertThat(sessions.saved).isNull();
    }

    private static BffSession sessionWithToken(TokenResponse token) {
        Instant now = Instant.now();
        return new BffSession(
                "session-1",
                token,
                new UserInfo("user-1", "Demo User", "user@example.com", Map.of("sub", "user-1")),
                now,
                now
        );
    }

    private static TokenResponse token(String accessToken, String refreshToken, Instant expiresAt) {
        return token(accessToken, refreshToken, expiresAt, "nonce");
    }

    private static TokenResponse token(String accessToken, String refreshToken, Instant expiresAt, String nonce) {
        return new TokenResponse(
                accessToken,
                "Bearer",
                refreshToken,
                idToken(nonce),
                "openid profile",
                expiresAt,
                Map.of("access_token", accessToken)
        );
    }

    private static final class CapturingAuthAdapter implements AuthAdapter {
        private final String tokenNonce;
        private String refreshedWith;

        private CapturingAuthAdapter() {
            this("nonce");
        }

        private CapturingAuthAdapter(String tokenNonce) {
            this.tokenNonce = tokenNonce;
        }

        @Override
        public AuthorizationRequest createAuthorizationRequest(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri) {
            return new AuthorizationRequest(
                    URI.create("http://localhost/login"),
                    redirectUri,
                    "state",
                    "nonce",
                    "verifier",
                    "challenge",
                    originalOrigin,
                    originalPath,
                    initPageUri
            );
        }

        @Override
        public TokenResponse exchangeCode(String code, String codeVerifier, URI redirectUri) {
            return token("exchanged-access-token", "refresh-token", Instant.now().plusSeconds(600), tokenNonce);
        }

        @Override
        public TokenResponse refreshToken(String refreshToken) {
            this.refreshedWith = refreshToken;
            return token("new-access-token", "new-refresh-token", Instant.now().plusSeconds(600));
        }

    }

    private static final class BlockingAuthAdapter implements AuthAdapter {
        private final CountDownLatch refreshStarted = new CountDownLatch(1);
        private final CountDownLatch continueRefresh = new CountDownLatch(1);
        private final AtomicInteger refreshCount = new AtomicInteger();

        @Override
        public AuthorizationRequest createAuthorizationRequest(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public TokenResponse exchangeCode(String code, String codeVerifier, URI redirectUri) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public TokenResponse refreshToken(String refreshToken) {
            refreshCount.incrementAndGet();
            refreshStarted.countDown();
            try {
                assertThat(continueRefresh.await(3, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new OAuthOidcClientException("interrupted");
            }
            return token("new-access-token", "new-refresh-token", Instant.now().plusSeconds(600));
        }

    }

    private static String idToken(String nonce) {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"sub\":\"user-1\",\"nonce\":\"" + nonce + "\"}");
        return header + "." + payload + ".";
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class CapturingSessionStore implements BffSessionStore {
        private BffSession session;
        private BffSession saved;

        private CapturingSessionStore(BffSession session) {
            this.session = session;
        }

        @Override
        public synchronized void save(BffSession session) {
            this.session = session;
            this.saved = session;
        }

        @Override
        public synchronized Optional<BffSession> find(String sessionId) {
            if (!"session-1".equals(sessionId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(session);
        }

        @Override
        public synchronized void delete(String sessionId) {
            this.session = null;
        }
    }

    private static final class CapturingAuthorizationRequestStore implements AuthorizationRequestStore {
        private AuthorizationRequest request;

        @Override
        public void save(AuthorizationRequest request) {
            this.request = request;
        }

        @Override
        public Optional<AuthorizationRequest> remove(String state) {
            if (request == null || !request.state().equals(state)) {
                return Optional.empty();
            }
            AuthorizationRequest removed = request;
            request = null;
            return Optional.of(removed);
        }
    }
}
