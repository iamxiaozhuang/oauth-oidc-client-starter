package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class LoginRedirectContextResolver {
    private LoginRedirectContextResolver() {
    }

    static LoginRedirectContext resolve(HttpServletRequest request, OAuthOidcClientProperties properties) {
        return resolve(
                request::getHeader,
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                pathWithQuery(request.getRequestURI(), request.getQueryString()),
                request.getParameter(properties.getOriginalUrlParam()),
                properties
        );
    }

    static LoginRedirectContext resolve(ServerHttpRequest request, OAuthOidcClientProperties properties) {
        HttpHeaders headers = request.getHeaders();
        URI uri = request.getURI();
        return resolve(
                headers::getFirst,
                uri.getScheme(),
                uri.getHost(),
                uri.getPort(),
                pathWithQuery(uri.getRawPath(), uri.getRawQuery()),
                request.getQueryParams().getFirst(properties.getOriginalUrlParam()),
                properties
        );
    }

    private static LoginRedirectContext resolve(
            Function<String, String> header,
            String requestScheme,
            String requestHost,
            int requestPort,
            String requestPath,
            String targetPath,
            OAuthOidcClientProperties properties
    ) {
        URI originalOrigin = originalOrigin(header, requestScheme, requestHost, requestPort, properties);
        validateAllowedOrigin(originalOrigin, properties.getAllowedRedirectHosts());
        String originalPath = originalPath(targetPath, requestPath);
        URI redirectUri = originWithPath(originalOrigin, properties.getCallbackPath(), "callback-path");
        URI initPageUri = originWithPath(originalOrigin, properties.getLoginSuccessPath(), "login-success-path");
        return new LoginRedirectContext(
                originalOrigin,
                originalPath,
                redirectUri,
                initPageUri
        );
    }

    private static URI originalOrigin(
            Function<String, String> header,
            String requestScheme,
            String requestHost,
            int requestPort,
            OAuthOidcClientProperties properties
    ) {
        String scheme = requestScheme;
        String host = hostWithPort(requestHost, requestPort, scheme);
        if (properties.isTrustForwardedHeaders()) {
            Forwarded forwarded = forwarded(header);
            scheme = firstText(forwarded.proto(), firstText(header.apply("X-Forwarded-Proto"), scheme));
            host = firstText(forwarded.host(), firstText(header.apply("X-Forwarded-Host"), host));
            host = firstForwardedValue(host);
        }
        return URI.create(scheme + "://" + host);
    }

    private static void validateAllowedOrigin(URI originalOrigin, List<String> allowedHosts) {
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            throw new OAuthOidcClientException("allowed-redirect-hosts must contain at least one host");
        }
        String originHost = hostPort(originalOrigin);
        if (!isAllowedHost(originHost, allowedHosts)) {
            throw new OAuthOidcClientException("original origin host is not allowed: " + originHost);
        }
    }

    private static boolean isAllowedHost(String host, List<String> allowedHosts) {
        String normalized = normalizeHost(host);
        for (String allowedHost : allowedHosts) {
            if (normalized.equals(normalizeHost(allowedHost))) {
                return true;
            }
        }
        return false;
    }

    private static String hostWithPort(String host, int port, String scheme) {
        if (host == null || host.isBlank()) {
            throw new OAuthOidcClientException("request host is missing");
        }
        if (port <= 0 || isDefaultPort(scheme, port) || host.contains(":")) {
            return host;
        }
        return host + ":" + port;
    }

    private static String hostPort(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new OAuthOidcClientException("uri host is missing: " + uri);
        }
        if (uri.getPort() <= 0 || isDefaultPort(uri.getScheme(), uri.getPort())) {
            return host;
        }
        return host + ":" + uri.getPort();
    }

    private static String normalizeHost(String host) {
        return firstForwardedValue(host).toLowerCase(Locale.ROOT);
    }

    private static String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : firstForwardedValue(first);
    }

    private static String firstForwardedValue(String value) {
        if (value == null) {
            return null;
        }
        return value.split(",", 2)[0].trim();
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    static URI originWithPath(URI origin, String path, String field) {
        String safePath = requireRelativePath(path, field);
        return URI.create(origin.toString() + safePath);
    }

    private static String originalPath(String targetPath, String requestPath) {
        if (targetPath != null && !targetPath.isBlank()) {
            return requireRelativePath(targetPath, "target");
        }
        return requireRelativePath(requestPath, "request path");
    }

    private static String requireRelativePath(String path, String field) {
        if (path == null || path.isBlank()) {
            throw new OAuthOidcClientException(field + " is required");
        }
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\") || containsControlCharacter(path)) {
            throw new OAuthOidcClientException(field + " must be a site-relative path");
        }
        try {
            URI uri = URI.create(path);
            if (uri.isAbsolute() || uri.getHost() != null || uri.getRawSchemeSpecificPart() != null && path.startsWith("//")) {
                throw new OAuthOidcClientException(field + " must be a site-relative path");
            }
        } catch (IllegalArgumentException ex) {
            throw new OAuthOidcClientException(field + " must be a valid URI path", ex);
        }
        return path;
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7f);
    }

    private static String pathWithQuery(String path, String query) {
        String safePath = path == null || path.isBlank() ? "/" : path;
        return query == null || query.isBlank() ? safePath : safePath + "?" + query;
    }

    private static Forwarded forwarded(Function<String, String> header) {
        String value = header.apply("Forwarded");
        if (value == null || value.isBlank()) {
            return new Forwarded(null, null);
        }
        String first = firstForwardedValue(value);
        String proto = null;
        String host = null;
        for (String part : first.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String name = pair[0].trim().toLowerCase(Locale.ROOT);
            String pairValue = stripQuotes(pair[1].trim());
            if ("proto".equals(name)) {
                proto = pairValue;
            } else if ("host".equals(name)) {
                host = pairValue;
            }
        }
        return new Forwarded(proto, host);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record Forwarded(String proto, String host) {
    }
}
