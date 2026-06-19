package io.github.oidcclient.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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
        URI initPageUri = URI.create("https://app-a.example.com/auth/init-page");

        runtime.beginLogin(redirectUri, originalOrigin, "/dashboard?tab=home", initPageUri);
        OAuthOidcClientCallbackResult result = runtime.completeCallback(
                "code",
                "state",
                redirectUri,
                originalOrigin,
                "target"
        );

        assertThat(result.initRedirectUri().toString())
                .isEqualTo("https://app-a.example.com/auth/init-page?target=%2Fdashboard%3Ftab%3Dhome");
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
        return new TokenResponse(
                accessToken,
                "Bearer",
                refreshToken,
                null,
                "openid profile",
                expiresAt,
                Map.of("access_token", accessToken)
        );
    }

    private static final class CapturingAuthAdapter implements AuthAdapter {
        private String refreshedWith;

        @Override
        public AuthorizationRequest createAuthorizationRequest(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri) {
            return new AuthorizationRequest(
                    URI.create("http://localhost/login"),
                    redirectUri,
                    "state",
                    "verifier",
                    "challenge",
                    originalOrigin,
                    originalPath,
                    initPageUri
            );
        }

        @Override
        public TokenResponse exchangeCode(String code, String codeVerifier, URI redirectUri) {
            return token("exchanged-access-token", "refresh-token", Instant.now().plusSeconds(600));
        }

        @Override
        public TokenResponse refreshToken(String refreshToken) {
            this.refreshedWith = refreshToken;
            return token("new-access-token", "new-refresh-token", Instant.now().plusSeconds(600));
        }

        @Override
        public UserInfo fetchUserInfo(String accessToken) {
            return new UserInfo("user-1", "Demo User", "user@example.com", Map.of("sub", "user-1"));
        }
    }

    private static final class CapturingSessionStore implements BffSessionStore {
        private BffSession session;
        private BffSession saved;

        private CapturingSessionStore(BffSession session) {
            this.session = session;
        }

        @Override
        public void save(BffSession session) {
            this.session = session;
            this.saved = session;
        }

        @Override
        public Optional<BffSession> find(String sessionId) {
            if (!"session-1".equals(sessionId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(session);
        }

        @Override
        public void delete(String sessionId) {
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
