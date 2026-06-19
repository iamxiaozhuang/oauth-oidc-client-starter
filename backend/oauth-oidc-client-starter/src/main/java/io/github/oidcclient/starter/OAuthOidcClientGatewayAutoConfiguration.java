package io.github.oidcclient.starter;

import io.github.oidcclient.client.OAuthOidcClientRuntime;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = OAuthOidcClientAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "oauth-oidc-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuthOidcClientGatewayAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    OAuthOidcClientReactiveController OAuthOidcClientReactiveController(
            OAuthOidcClientRuntime runtime,
            OAuthOidcClientProperties properties
    ) {
        return new OAuthOidcClientReactiveController(runtime, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuthOidcClientGatewayTokenRelayFilter OAuthOidcClientGatewayTokenRelayFilter(
            OAuthOidcClientRuntime runtime,
            OAuthOidcClientProperties properties
    ) {
        return new OAuthOidcClientGatewayTokenRelayFilter(runtime, properties);
    }
}
