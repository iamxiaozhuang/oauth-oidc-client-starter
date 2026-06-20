package io.github.oidcclient.starter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthOidcClientGatewayTokenRelayFilterTest {
    @Test
    void loginTargetPrefersSameOriginRefererPage() {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:5173/api/current-user")
                .header(HttpHeaders.REFERER, "http://localhost:5173/dashboard?tab=home")
                .build();

        assertThat(OAuthOidcClientGatewayTokenRelayFilter.loginTarget(request))
                .isEqualTo("/dashboard?tab=home");
    }

    @Test
    void loginTargetFallsBackToRequestPathWhenRefererIsCrossOrigin() {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:5173/api/current-user")
                .header(HttpHeaders.REFERER, "https://evil.example.com/dashboard")
                .build();

        assertThat(OAuthOidcClientGatewayTokenRelayFilter.loginTarget(request))
                .isEqualTo("/api/current-user");
    }
}
