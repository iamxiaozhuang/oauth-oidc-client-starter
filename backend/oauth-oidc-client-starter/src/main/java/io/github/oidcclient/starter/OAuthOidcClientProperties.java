package io.github.oidcclient.starter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "oauth-oidc-client")
public class OAuthOidcClientProperties {
    // 是否启用 starter 自动装配。
    private boolean enabled = true;
    // OAuth2/OIDC provider 的授权端点，浏览器会被重定向到这里。
    @NotNull
    private URI authorizationEndpoint;
    // OAuth2/OIDC provider 的 token 端点，只有后端会调用。
    @NotNull
    private URI tokenEndpoint;
    @NotBlank
    private String clientId;
    // confidential client 可配置 client secret；它只应存在于后端配置中。
    private String clientSecret;
    // provider 回调到后端的地址，必须和 provider 控制台配置完全一致。
    private URI redirectUri;
    // callback 地址固定为站内 path，运行时使用首次请求的域名入口拼接完整 redirect_uri。
    @NotBlank
    private String callbackPath = "/oauth/callback";
    // 登录成功后先跳初始化页面，由页面调用业务初始化接口，再回到 target。
    @NotBlank
    private String loginSuccessPath = "/";
    // 注销成功后跳转到业务系统自己的退出完成页。
    @NotBlank
    private String logoutSuccessPath = "/logout";
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._~-]+")
    private String targetParam = "target";
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._~-]+")
    private String originalUrlParam = "target";
    // 允许用于动态 redirect_uri 的访问域名，必须显式白名单。
    @NotEmpty
    private List<@NotBlank String> allowedRedirectHosts = List.of("localhost:5173");
    @NotEmpty
    private List<@NotBlank String> scopes = List.of("openid", "profile", "email");
    // 透传给授权端点的额外参数，例如 Google 的 access_type=offline、prompt=consent。
    @NotNull
    private Map<@NotBlank String, @NotBlank String> authorizationParameters = Map.of();
    @NotBlank
    @Pattern(regexp = "[^\\s;,=]+")
    private String cookieName = "oauth-session";
    @NotBlank
    private String cookiePath = "/";
    private boolean secureCookie;
    // HttpOnly 默认开启，避免前端脚本读取 BFF session id。
    private boolean httpOnlyCookie = true;
    @NotBlank
    @Pattern(regexp = "(?i)Strict|Lax|None")
    private String sameSite = "Lax";
    // 临时授权请求的有效期；过期后 callback 会因为 state 缺失而失败。
    @NotNull
    private Duration authorizationRequestTtl = Duration.ofMinutes(5);
    // Redis 中 BFF session 数据的有效期；浏览器 cookie 是会话级 cookie，关闭浏览器即丢失。
    @NotNull
    private Duration redisSessionTtl = Duration.ofHours(12);
    // access token 过期前的提前刷新窗口。
    @NotNull
    private Duration refreshSkew = Duration.ofSeconds(60);
    // 分布式刷新锁持有时间，应覆盖一次 token refresh 的最长预期耗时。
    @NotNull
    private Duration refreshLockTtl = Duration.ofSeconds(30);
    // 等待其他 Gateway 实例完成刷新时的最大等待时间。
    @NotNull
    private Duration refreshLockWaitTimeout = Duration.ofSeconds(10);
    // 等待刷新锁时的重试间隔。
    @NotNull
    private Duration refreshLockRetryInterval = Duration.ofMillis(100);
    // 反向代理部署时从 Forwarded / X-Forwarded-* 头解析外部访问域名。
    private boolean trustForwardedHeaders = true;
    // Gateway BFF 默认保护所有非公开路径；认证以服务端 BFF session 为准，而不是浏览器传来的 JWT。
    @NotNull
    private List<@NotBlank String> protectedPathPrefixes = List.of("/");
    @NotNull
    private List<@NotBlank String> publicPathPrefixes = List.of("/oauth/");
    @Valid
    @NotNull
    private Redis redis = new Redis();
    @Valid
    @NotNull
    private Http http = new Http();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(URI authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public URI getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(URI tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getCallbackPath() {
        return callbackPath;
    }

    public void setCallbackPath(String callbackPath) {
        this.callbackPath = callbackPath;
    }

    public String getLoginSuccessPath() {
        return loginSuccessPath;
    }

    public void setLoginSuccessPath(String loginSuccessPath) {
        this.loginSuccessPath = loginSuccessPath;
    }

    public String getLogoutSuccessPath() {
        return logoutSuccessPath;
    }

    public void setLogoutSuccessPath(String logoutSuccessPath) {
        this.logoutSuccessPath = logoutSuccessPath;
    }

    public String getTargetParam() {
        return targetParam;
    }

    public void setTargetParam(String targetParam) {
        this.targetParam = targetParam;
    }

    public String getOriginalUrlParam() {
        return originalUrlParam;
    }

    public void setOriginalUrlParam(String originalUrlParam) {
        this.originalUrlParam = originalUrlParam;
    }

    public List<String> getAllowedRedirectHosts() {
        return allowedRedirectHosts;
    }

    public void setAllowedRedirectHosts(List<String> allowedRedirectHosts) {
        this.allowedRedirectHosts = allowedRedirectHosts;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Map<String, String> getAuthorizationParameters() {
        return authorizationParameters;
    }

    public void setAuthorizationParameters(Map<String, String> authorizationParameters) {
        this.authorizationParameters = authorizationParameters;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public boolean isHttpOnlyCookie() {
        return httpOnlyCookie;
    }

    public void setHttpOnlyCookie(boolean httpOnlyCookie) {
        this.httpOnlyCookie = httpOnlyCookie;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public Duration getAuthorizationRequestTtl() {
        return authorizationRequestTtl;
    }

    public void setAuthorizationRequestTtl(Duration authorizationRequestTtl) {
        this.authorizationRequestTtl = authorizationRequestTtl;
    }

    public Duration getRedisSessionTtl() {
        return redisSessionTtl;
    }

    public void setRedisSessionTtl(Duration redisSessionTtl) {
        this.redisSessionTtl = redisSessionTtl;
    }

    public Duration getRefreshSkew() {
        return refreshSkew;
    }

    public void setRefreshSkew(Duration refreshSkew) {
        this.refreshSkew = refreshSkew;
    }

    public Duration getRefreshLockTtl() {
        return refreshLockTtl;
    }

    public void setRefreshLockTtl(Duration refreshLockTtl) {
        this.refreshLockTtl = refreshLockTtl;
    }

    public Duration getRefreshLockWaitTimeout() {
        return refreshLockWaitTimeout;
    }

    public void setRefreshLockWaitTimeout(Duration refreshLockWaitTimeout) {
        this.refreshLockWaitTimeout = refreshLockWaitTimeout;
    }

    public Duration getRefreshLockRetryInterval() {
        return refreshLockRetryInterval;
    }

    public void setRefreshLockRetryInterval(Duration refreshLockRetryInterval) {
        this.refreshLockRetryInterval = refreshLockRetryInterval;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public List<String> getProtectedPathPrefixes() {
        return protectedPathPrefixes;
    }

    public void setProtectedPathPrefixes(List<String> protectedPathPrefixes) {
        this.protectedPathPrefixes = protectedPathPrefixes;
    }

    public List<String> getPublicPathPrefixes() {
        return publicPathPrefixes;
    }

    public void setPublicPathPrefixes(List<String> publicPathPrefixes) {
        this.publicPathPrefixes = publicPathPrefixes;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    @AssertTrue(message = "authorization-endpoint must be an absolute http(s) URI")
    public boolean isAuthorizationEndpointValid() {
        return isHttpUri(authorizationEndpoint);
    }

    @AssertTrue(message = "token-endpoint must be an absolute http(s) URI")
    public boolean isTokenEndpointValid() {
        return isHttpUri(tokenEndpoint);
    }

    @AssertTrue(message = "redirect-uri must be empty or an absolute http(s) URI")
    public boolean isRedirectUriValid() {
        return redirectUri == null || isHttpUri(redirectUri);
    }

    @AssertTrue(message = "callback-path, login-success-path, logout-success-path, and cookie-path must be site-relative paths")
    public boolean isSiteRelativePathsValid() {
        return isSiteRelativePath(callbackPath)
                && isSiteRelativePath(loginSuccessPath)
                && isSiteRelativePath(logoutSuccessPath)
                && isSiteRelativePath(cookiePath);
    }

    @AssertTrue(message = "protected-path-prefixes and public-path-prefixes must contain only site-relative paths")
    public boolean isPathPrefixesValid() {
        return allSiteRelativePaths(protectedPathPrefixes) && allSiteRelativePaths(publicPathPrefixes);
    }

    @AssertTrue(message = "allowed-redirect-hosts must contain host[:port] values without scheme or path")
    public boolean isAllowedRedirectHostsValid() {
        return allowedRedirectHosts != null && allowedRedirectHosts.stream().allMatch(OAuthOidcClientProperties::isHostPort);
    }

    @AssertTrue(message = "authorization-request-ttl, redis-session-ttl, refresh-skew, and refresh lock durations must be positive")
    public boolean isDurationsValid() {
        return isPositive(authorizationRequestTtl)
                && isPositive(redisSessionTtl)
                && isPositive(refreshSkew)
                && isPositive(refreshLockTtl)
                && isPositive(refreshLockWaitTimeout)
                && isPositive(refreshLockRetryInterval);
    }

    @AssertTrue(message = "refresh-lock-retry-interval must not be greater than refresh-lock-wait-timeout")
    public boolean isRefreshLockRetryIntervalValid() {
        return refreshLockRetryInterval == null
                || refreshLockWaitTimeout == null
                || !refreshLockRetryInterval.minus(refreshLockWaitTimeout).isPositive();
    }

    @AssertTrue(message = "same-site=None requires secure-cookie=true")
    public boolean isSameSiteSecureValid() {
        return !"None".equalsIgnoreCase(sameSite) || secureCookie;
    }

    public static class Redis {
        @NotBlank
        private String keyPrefix = "oauth-oidc-client";

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Http {
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(5);
        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(10);
        private URI proxyUrl;

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public URI getProxyUrl() {
            return proxyUrl;
        }

        public void setProxyUrl(URI proxyUrl) {
            this.proxyUrl = proxyUrl;
        }

        @AssertTrue(message = "connect-timeout and request-timeout must be positive")
        public boolean isTimeoutsValid() {
            return isPositive(connectTimeout) && isPositive(requestTimeout);
        }

        @AssertTrue(message = "proxy-url must be empty or an absolute http(s) URI")
        public boolean isProxyUrlValid() {
            return proxyUrl == null || isHttpUri(proxyUrl);
        }
    }

    private static boolean isHttpUri(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
            return false;
        }
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private static boolean isSiteRelativePath(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/") || value.startsWith("//")) {
            return false;
        }
        try {
            return !URI.create(value).isAbsolute();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean allSiteRelativePaths(List<String> values) {
        return values != null && values.stream().allMatch(OAuthOidcClientProperties::isSiteRelativePath);
    }

    private static boolean isHostPort(String value) {
        if (value == null || value.isBlank() || value.contains("/") || value.contains("@")) {
            return false;
        }
        try {
            URI uri = URI.create("http://" + value);
            String path = uri.getRawPath();
            return uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && (path == null || path.isBlank())
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }
}
