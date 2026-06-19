package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientRuntime;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {
    private final OAuthOidcClientRuntime runtime;
    private final OAuthOidcClientProperties properties;

    public CurrentUserIdArgumentResolver(OAuthOidcClientRuntime runtime, OAuthOidcClientProperties properties) {
        this.runtime = runtime;
        this.properties = properties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 只支持 @CurrentUserId String，保持解析器行为明确，避免误注入复杂对象。
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "request is missing");
        }
        // Controller 方法无需手动解析 cookie，可直接通过 @CurrentUserId 获取后端 session 中的用户 ID。
        String sessionId = CookieSessionIdResolver.resolve(request, properties.getCookieName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "BFF session is missing"));
        return runtime.currentUserId(sessionId);
    }
}
