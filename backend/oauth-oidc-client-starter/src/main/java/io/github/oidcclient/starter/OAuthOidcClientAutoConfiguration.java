package io.github.oidcclient.starter;

import io.github.oidcclient.client.AuthorizationRequestStore;
import io.github.oidcclient.client.BffSessionStore;
import io.github.oidcclient.client.OAuthOidcClientClient;
import io.github.oidcclient.client.OAuthOidcClientConfig;
import io.github.oidcclient.client.OAuthOidcClientRuntime;
import io.github.oidcclient.client.RedisAuthorizationRequestStore;
import io.github.oidcclient.client.RedisBffSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;

@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "oauth-oidc-client", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OAuthOidcClientProperties.class)
public class OAuthOidcClientAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(OAuthOidcClientAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    OAuthOidcClientConfig OAuthOidcClientConfig(OAuthOidcClientProperties properties) {
        // 把 Spring Boot 配置属性转换成核心 client 使用的不可变配置对象。
        return OAuthOidcClientConfig.builder()
                .authorizationEndpoint(properties.getAuthorizationEndpoint())
                .tokenEndpoint(properties.getTokenEndpoint())
                .userInfoEndpoint(properties.getUserInfoEndpoint())
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .redirectUri(properties.getRedirectUri())
                .scopes(properties.getScopes())
                .authorizationParameters(properties.getAuthorizationParameters())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    OAuthOidcClientClient OAuthOidcClientClient(OAuthOidcClientConfig config, OAuthOidcClientProperties properties) {
        // OAuth2 请求使用独立 HttpClient，方便配置超时和代理，不依赖宿主应用的 WebClient/RestTemplate。
        return new OAuthOidcClientClient(
                config,
                OAuthOidcClientHttpClient(properties),
                properties.getHttp().getRequestTimeout()
        );
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    AuthorizationRequestStore redisAuthorizationRequestStore(
            StringRedisTemplate redis,
            ObjectProvider<ObjectMapper> objectMapper,
            OAuthOidcClientProperties properties
    ) {
        // 授权请求是短生命周期数据，保存 state/code_verifier/code_challenge，用于 callback 校验和换 token。
        return new RedisAuthorizationRequestStore(
                redis,
                redisStoreObjectMapper(objectMapper),
                properties.getAuthorizationRequestTtl(),
                properties.getRedis().getKeyPrefix()
        );
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    BffSessionStore redisBffSessionStore(
            StringRedisTemplate redis,
            ObjectProvider<ObjectMapper> objectMapper,
            OAuthOidcClientProperties properties
    ) {
        // BFF session 保存 token 和 userinfo，统一使用 Redis 以支持多实例部署。
        return new RedisBffSessionStore(
                redis,
                redisStoreObjectMapper(objectMapper),
                properties.getRedisSessionTtl(),
                properties.getRedis().getKeyPrefix()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    OAuthOidcClientRuntime OAuthOidcClientRuntime(
            OAuthOidcClientClient client,
            AuthorizationRequestStore authorizationRequests,
            BffSessionStore sessions,
            OAuthOidcClientProperties properties
    ) {
        // Runtime 串联登录、回调、会话、刷新和注销，是 starter 暴露端点背后的核心协调层。
        return new OAuthOidcClientRuntime(client, authorizationRequests, sessions, properties.getRefreshSkew());
    }

    @Bean
    ApplicationRunner OAuthOidcClientStoreLogger(
            AuthorizationRequestStore authorizationRequests,
            BffSessionStore sessions,
            ObjectProvider<StringRedisTemplate> redis,
            OAuthOidcClientProperties properties
    ) {
        return args -> log.info(
                "OAuth2/OIDC client stores initialized: authorizationRequestStore={}, bffSessionStore={}, stringRedisTemplate={}, redisKeyPrefix={}",
                authorizationRequests.getClass().getName(),
                sessions.getClass().getName(),
                redis.getIfAvailable() != null,
                properties.getRedis().getKeyPrefix()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    OAuthOidcClientController OAuthOidcClientController(
            OAuthOidcClientRuntime runtime,
            OAuthOidcClientProperties properties
    ) {
        return new OAuthOidcClientController(runtime, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    CurrentUserIdArgumentResolver currentUserIdArgumentResolver(
            OAuthOidcClientRuntime runtime,
            OAuthOidcClientProperties properties
    ) {
        return new CurrentUserIdArgumentResolver(runtime, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    OAuthOidcClientWebMvcConfigurer OAuthOidcClientWebMvcConfigurer(
            CurrentUserIdArgumentResolver currentUserIdArgumentResolver
    ) {
        return new OAuthOidcClientWebMvcConfigurer(currentUserIdArgumentResolver);
    }

    private static ObjectMapper redisStoreObjectMapper(ObjectProvider<ObjectMapper> objectMapper) {
        // 优先复用宿主应用的 ObjectMapper，保证 Java Time 等模块配置一致。
        return objectMapper.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
    }

    private static HttpClient OAuthOidcClientHttpClient(OAuthOidcClientProperties properties) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(properties.getHttp().getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER);
        URI proxyUrl = properties.getHttp().getProxyUrl();
        if (proxyUrl != null) {
            // Java 进程不会自动继承浏览器代理；需要访问外部 OAuth provider 时可在配置中显式指定。
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyUrl.getHost(), proxyPort(proxyUrl))));
            String userInfo = proxyUrl.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                char[] password = parts.length == 2 ? parts[1].toCharArray() : new char[0];
                builder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(parts[0], password);
                    }
                });
            }
        }
        return builder.build();
    }

    private static int proxyPort(URI proxyUrl) {
        if (proxyUrl.getPort() > 0) {
            return proxyUrl.getPort();
        }
        return "https".equalsIgnoreCase(proxyUrl.getScheme()) ? 443 : 80;
    }
}
