package io.github.oidcclient.starter;

import io.github.oidcclient.client.AuthorizationRequestStore;
import io.github.oidcclient.client.BffSessionStore;
import io.github.oidcclient.client.OAuthOidcClientRuntime;
import io.github.oidcclient.client.RedisAuthorizationRequestStore;
import io.github.oidcclient.client.RedisBffSessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OAuthOidcClientAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ServletWebServerFactoryAutoConfiguration.class,
                    DispatcherServletAutoConfiguration.class,
                    OAuthOidcClientAutoConfiguration.class
            ))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withPropertyValues(
                    "oauth-oidc-client.authorization-endpoint=http://localhost:9001/oauth2/authorize",
                    "oauth-oidc-client.token-endpoint=http://localhost:9001/oauth2/token",
                    "oauth-oidc-client.user-info-endpoint=http://localhost:9001/oauth2/userinfo",
                    "oauth-oidc-client.client-id=demo-client",
                    "oauth-oidc-client.client-secret=demo-secret",
                    "oauth-oidc-client.callback-path=/oauth/callback",
                    "oauth-oidc-client.login-success-path=/auth/init-page",
                    "oauth-oidc-client.redis-session-ttl=12h",
                    "oauth-oidc-client.allowed-redirect-hosts=localhost:5173"
            );

    @Test
    void autoConfigurationRegistersStarterBeansWithRedisStores() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OAuthOidcClientRuntime.class);
            assertThat(context).hasSingleBean(OAuthOidcClientController.class);
            assertThat(context).hasSingleBean(AuthorizationRequestStore.class);
            assertThat(context).hasSingleBean(BffSessionStore.class);
            assertThat(context).hasSingleBean(RedisAuthorizationRequestStore.class);
            assertThat(context).hasSingleBean(RedisBffSessionStore.class);
            assertThat(context).hasSingleBean(CurrentUserIdArgumentResolver.class);
        });
    }

}
