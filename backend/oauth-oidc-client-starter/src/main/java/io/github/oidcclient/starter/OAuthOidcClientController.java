package io.github.oidcclient.starter;

import io.github.oidcclient.client.BffSession;
import io.github.oidcclient.client.OAuthOidcClientCallbackResult;
import io.github.oidcclient.client.OAuthOidcClientRuntime;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OAuthOidcClientController {
    private final OAuthOidcClientRuntime runtime;
    private final OAuthOidcClientProperties properties;

    public OAuthOidcClientController(OAuthOidcClientRuntime runtime, OAuthOidcClientProperties properties) {
        this.runtime = runtime;
        this.properties = properties;
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        // 登录入口只负责把浏览器重定向到授权服务器；state 和 code_verifier 已在后端保存。
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
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 回调阶段必须先校验 state，再使用后端保存的 code_verifier 换 token。
        // 浏览器只拿到 BFF session cookie，不接触 access token / refresh token。
        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);
        OAuthOidcClientCallbackResult result = runtime.completeCallback(
                code,
                state,
                context.redirectUri(),
                context.originalOrigin(),
                properties.getTargetParam()
        );
        BffSession session = result.session();
        CookieSessionIdResolver.write(response, properties, session.id());
        return redirect(result.initRedirectUri());
    }

    @GetMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 注销需要同时删除后端 session 和浏览器 cookie，避免留下单边状态。
        sessionId(request).ifPresent(runtime::logout);
        CookieSessionIdResolver.clear(response, properties);
        LoginRedirectContext context = LoginRedirectContextResolver.resolve(request, properties);
        return redirect(logoutSuccessUri(context.originalOrigin()));
    }

    private java.util.Optional<String> sessionId(HttpServletRequest request) {
        return CookieSessionIdResolver.resolve(request, properties.getCookieName());
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
