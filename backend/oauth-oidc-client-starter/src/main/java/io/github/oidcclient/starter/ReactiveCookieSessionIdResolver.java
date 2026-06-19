package io.github.oidcclient.starter;

import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Optional;

final class ReactiveCookieSessionIdResolver {
    private ReactiveCookieSessionIdResolver() {
    }

    static Optional<String> resolve(ServerHttpRequest request, String cookieName) {
        return Optional.ofNullable(request.getCookies().getFirst(cookieName))
                .map(org.springframework.http.HttpCookie::getValue)
                .filter(value -> !value.isBlank());
    }

    static void write(ServerHttpResponse response, OAuthOidcClientProperties properties, String sessionId) {
        response.addCookie(baseCookie(properties, sessionId)
                .maxAge(-1)
                .build());
    }

    static void clear(ServerHttpResponse response, OAuthOidcClientProperties properties) {
        response.addCookie(baseCookie(properties, "")
                .maxAge(0)
                .build());
    }

    private static ResponseCookie.ResponseCookieBuilder baseCookie(
            OAuthOidcClientProperties properties,
            String value
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(properties.isHttpOnlyCookie())
                .secure(properties.isSecureCookie())
                .path(properties.getCookiePath());
        if (properties.getSameSite() != null && !properties.getSameSite().isBlank()) {
            builder.sameSite(properties.getSameSite());
        }
        return builder;
    }
}
