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
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String target = query == null || query.isBlank() ? path : path + "?" + query;
        return FormCodec.appendQuery(URI.create(LOGIN_PATH), Map.of(properties.getOriginalUrlParam(), target));
    }
}
