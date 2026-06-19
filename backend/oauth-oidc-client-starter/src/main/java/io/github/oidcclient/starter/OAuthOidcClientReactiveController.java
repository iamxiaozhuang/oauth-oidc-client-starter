package io.github.oidcclient.starter;

import io.github.oidcclient.client.BffSession;
import io.github.oidcclient.client.OAuthOidcClientCallbackResult;
import io.github.oidcclient.client.OAuthOidcClientRuntime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OAuthOidcClientReactiveController {
    private final OAuthOidcClientRuntime runtime;
    private final OAuthOidcClientProperties properties;

    public OAuthOidcClientReactiveController(OAuthOidcClientRuntime runtime, OAuthOidcClientProperties properties) {
        this.runtime = runtime;
        this.properties = properties;
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> login(ServerHttpRequest request) {
        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);
        return redirect(runtime.beginLogin(
                context.redirectUri(),
                context.originalOrigin(),
                context.originalPath(),
                context.initPageUri()
        ));
    }

    @GetMapping("${oauth-oidc-client.callback-path:/oauth/callback}")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);
        OAuthOidcClientCallbackResult result = runtime.completeCallback(
                code,
                state,
                context.redirectUri(),
                context.originalOrigin(),
                properties.getTargetParam()
        );
        BffSession session = result.session();
        ReactiveCookieSessionIdResolver.write(response, properties, session.id());
        return redirect(result.initRedirectUri());
    }

    @GetMapping("/oauth/logout")
    public ResponseEntity<Void> logout(ServerHttpRequest request, ServerHttpResponse response) {
        sessionId(request).ifPresent(runtime::logout);
        ReactiveCookieSessionIdResolver.clear(response, properties);
        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);
        return redirect(logoutSuccessUri(context.originalOrigin()));
    }

    private java.util.Optional<String> sessionId(ServerHttpRequest request) {
        return ReactiveCookieSessionIdResolver.resolve(request, properties.getCookieName());
    }

    private URI logoutSuccessUri(URI originalOrigin) {
        return LoginRedirectContextResolver.originWithPath(
                originalOrigin,
                properties.getLogoutSuccessPath(),
                "logout-success-path"
        );
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }
}
