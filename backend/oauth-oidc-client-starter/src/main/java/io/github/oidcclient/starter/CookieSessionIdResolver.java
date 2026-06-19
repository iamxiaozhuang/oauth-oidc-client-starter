package io.github.oidcclient.starter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

final class CookieSessionIdResolver {
    private CookieSessionIdResolver() {
    }

    static Optional<String> resolve(HttpServletRequest request, String cookieName) {
        // 只解析 session id；真正的 token 和用户信息仍在后端 session store 中。
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    static void write(HttpServletResponse response, OAuthOidcClientProperties properties, String sessionId) {
        Cookie cookie = baseCookie(properties, sessionId);
        // -1 表示浏览器会话级 cookie；服务端 session 数据由 redisSessionTtl 自动过期。
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    static void clear(HttpServletResponse response, OAuthOidcClientProperties properties) {
        Cookie cookie = baseCookie(properties, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private static Cookie baseCookie(OAuthOidcClientProperties properties, String value) {
        Cookie cookie = new Cookie(properties.getCookieName(), value);
        cookie.setHttpOnly(properties.isHttpOnlyCookie());
        cookie.setSecure(properties.isSecureCookie());
        cookie.setPath(properties.getCookiePath());
        if (properties.getSameSite() != null && !properties.getSameSite().isBlank()) {
            // SameSite=Lax 默认适合 OAuth 重定向回调；跨站嵌入场景可由接入方调整。
            cookie.setAttribute("SameSite", properties.getSameSite());
        }
        return cookie;
    }
}
