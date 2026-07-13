package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OAuthOidcClientReactiveControllerTest {
    @Test
    void logoutDeletesSessionClearsCookieAndRedirectsToConfiguredPage() {
        OAuthOidcClientRuntime runtime = mock(OAuthOidcClientRuntime.class);
        OAuthOidcClientProperties properties = new OAuthOidcClientProperties();
        properties.setAllowedRedirectHosts(List.of("localhost:5173"));
        properties.setLogoutSuccessPath("/logout-page");
        properties.setRedisSessionTtl(Duration.ofHours(12));
        OAuthOidcClientReactiveController controller = new OAuthOidcClientReactiveController(runtime, properties);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost:5173/api/logout")
                .cookie(new HttpCookie("oauth-session", "session-1"))
                .build();
        MockServerHttpResponse response = new MockServerHttpResponse();

        var result = controller.logout(request, response);

        verify(runtime).logout("session-1");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:5173/logout-page"));
        ResponseCookie clearedCookie = response.getCookies().getFirst("oauth-session");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getValue()).isEmpty();
        assertThat(clearedCookie.getMaxAge()).isZero();
        assertThat(clearedCookie.isHttpOnly()).isTrue();
        assertThat(clearedCookie.getPath()).isEqualTo("/");
    }
}
