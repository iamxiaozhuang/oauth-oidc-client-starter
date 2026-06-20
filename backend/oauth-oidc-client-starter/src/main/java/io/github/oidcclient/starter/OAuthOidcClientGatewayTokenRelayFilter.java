package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientException;
import io.github.oidcclient.client.OAuthOidcClientRuntime;
import io.github.oidcclient.client.TokenResponse;
import io.github.oidcclient.client.internal.FormCodec;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

public class OAuthOidcClientGatewayTokenRelayFilter implements GlobalFilter, Ordered {
    private static final int ORDER = -100;
    private static final String LOGIN_PATH = "/oauth/login";
    private static final String LOGIN_HEADER = "X-Login-Path";

    private final OAuthOidcClientRuntime runtime;
    private final OAuthOidcClientProperties properties;

    public OAuthOidcClientGatewayTokenRelayFilter(
            OAuthOidcClientRuntime runtime,
            OAuthOidcClientProperties properties
    ) {
        this.runtime = runtime;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!requiresAuthentication(path)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().beforeCommit(() -> {
            if (HttpStatus.UNAUTHORIZED.equals(exchange.getResponse().getStatusCode())) {
                exchange.getResponse().getHeaders().set(LOGIN_HEADER, loginUri(exchange).toString());
            }
            return Mono.empty();
        });

        return Mono.defer(() -> ReactiveCookieSessionIdResolver
                .resolve(exchange.getRequest(), properties.getCookieName())
                .map(sessionId -> relayToken(exchange, chain, sessionId))
                .orElseGet(() -> redirectToLogin(exchange)));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private Mono<Void> relayToken(ServerWebExchange exchange, GatewayFilterChain chain, String sessionId) {
        try {
            // currentToken performs refresh-on-skew and persists the refreshed BFF session via the configured store.
            TokenResponse token = runtime.currentToken(sessionId);
            ServerWebExchange mutated = exchange.mutate()
                    .request(request -> request.headers(headers ->
                            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())))
                    .build();
            return chain.filter(mutated);
        } catch (OAuthOidcClientException ex) {
            ReactiveCookieSessionIdResolver.clear(exchange.getResponse(), properties);
            return redirectToLogin(exchange);
        }
    }

    private boolean requiresAuthentication(String path) {
        if (matchesPrefix(path, properties.getPublicPathPrefixes())) {
            return false;
        }
        return matchesPrefix(path, properties.getProtectedPathPrefixes());
    }

    private static boolean matchesPrefix(String path, java.util.List<String> prefixes) {
        if (prefixes == null) {
            return false;
        }
        String normalizedPath = path == null || path.isBlank() ? "/" : path;
        return prefixes.stream()
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .map(OAuthOidcClientGatewayTokenRelayFilter::normalizePrefix)
                .anyMatch(normalizedPath::startsWith);
    }

    private static String normalizePrefix(String prefix) {
        if ("/".equals(prefix)) {
            return "/";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
        URI loginUri = loginUri(exchange);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(LOGIN_HEADER, loginUri.toString());
        return exchange.getResponse().setComplete();
    }

    private URI loginUri(ServerWebExchange exchange) {
        String target = loginTarget(exchange.getRequest());
        return FormCodec.appendQuery(URI.create(LOGIN_PATH), Map.of(properties.getOriginalUrlParam(), target));
    }

    static String loginTarget(ServerHttpRequest request) {
        URI requestUri = request.getURI();
        String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
        if (referer != null && !referer.isBlank()) {
            URI refererUri = URI.create(referer);
            if (sameOrigin(requestUri, refererUri)) {
                return pathWithQuery(refererUri);
            }
        }
        return pathWithQuery(requestUri);
    }

    private static boolean sameOrigin(URI requestUri, URI refererUri) {
        return equalsIgnoreCase(requestUri.getScheme(), refererUri.getScheme())
                && equalsIgnoreCase(requestUri.getHost(), refererUri.getHost())
                && normalizedPort(requestUri) == normalizedPort(refererUri);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }

    private static int normalizedPort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String pathWithQuery(URI uri) {
        String path = uri.getRawPath();
        String safePath = path == null || path.isBlank() ? "/" : path;
        String query = uri.getRawQuery();
        return query == null || query.isBlank() ? safePath : safePath + "?" + query;
    }
}
