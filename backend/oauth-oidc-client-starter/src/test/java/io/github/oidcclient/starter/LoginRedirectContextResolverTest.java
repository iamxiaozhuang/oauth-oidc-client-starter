package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRedirectContextResolverTest {
    @Test
    void resolvesOriginalOriginAndOriginalPathFromLoginTarget() {
        OAuthOidcClientProperties properties = properties();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/login");
        request.setScheme("https");
        request.setServerName("app-a.example.com");
        request.setServerPort(443);
        request.setParameter("target", "/dashboard?tab=member");

        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);

        assertThat(context.originalOrigin().toString()).isEqualTo("https://app-a.example.com");
        assertThat(context.originalPath()).isEqualTo("/dashboard?tab=member");
        assertThat(context.redirectUri().toString()).isEqualTo("https://app-a.example.com/oauth/callback");
        assertThat(context.initPageUri().toString()).isEqualTo("https://app-a.example.com/auth/init-page");
    }

    @Test
    void rejectsExternalTarget() {
        OAuthOidcClientProperties properties = properties();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/login");
        request.setScheme("https");
        request.setServerName("app-a.example.com");
        request.setServerPort(443);
        request.setParameter("target", "https://evil.example.com/dashboard");

        assertThatThrownBy(() -> LoginRedirectContextResolver.resolve(request, properties))
                .isInstanceOf(OAuthOidcClientException.class)
                .hasMessageContaining("site-relative path");
    }

    private static OAuthOidcClientProperties properties() {
        OAuthOidcClientProperties properties = new OAuthOidcClientProperties();
        properties.setAllowedRedirectHosts(java.util.List.of("app-a.example.com"));
        properties.setCallbackPath("/oauth/callback");
        properties.setLoginSuccessPath("/auth/init-page");
        return properties;
    }
}
