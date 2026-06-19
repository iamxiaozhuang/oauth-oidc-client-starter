package io.github.oidcclient.starter;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

public final class OAuthOidcClientWebMvcConfigurer implements WebMvcConfigurer {
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    public OAuthOidcClientWebMvcConfigurer(CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
