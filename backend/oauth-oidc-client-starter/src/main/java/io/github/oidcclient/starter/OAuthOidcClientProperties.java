package io.github.oidcclient.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "oauth-oidc-client")
public class OAuthOidcClientProperties {
    // 是否启用 starter 自动装配。
    private boolean enabled = true;
    // OAuth2/OIDC provider 的授权端点，浏览器会被重定向到这里。
    private URI authorizationEndpoint;
    // OAuth2/OIDC provider 的 token 端点，只有后端会调用。
    private URI tokenEndpoint;
    // OIDC userinfo 端点，用于把 access token 换成当前用户 claims。
    private URI userInfoEndpoint;
    private String clientId;
    // confidential client 可配置 client secret；它只应存在于后端配置中。
    private String clientSecret;
    // provider 回调到后端的地址，必须和 provider 控制台配置完全一致。
    private URI redirectUri;
    // callback 地址固定为站内 path，运行时使用首次请求的域名入口拼接完整 redirect_uri。
    private String callbackPath = "/oauth/callback";
    // 登录成功后先跳初始化页面，由页面调用业务初始化接口，再回到 target。
    private String loginSuccessPath = "/";
    // 注销成功后跳转到业务系统自己的退出完成页。
    private String logoutSuccessPath = "/logout";
    private String targetParam = "target";
    private String originalUrlParam = "target";
    // 允许用于动态 redirect_uri 的访问域名，必须显式白名单。
    private List<String> allowedRedirectHosts = List.of("localhost:5173");
    private List<String> scopes = List.of("openid", "profile", "email");
    // 透传给授权端点的额外参数，例如 Google 的 access_type=offline、prompt=consent。
    private Map<String, String> authorizationParameters = Map.of();
    private String cookieName = "oauth-session";
    private String cookiePath = "/";
    private boolean secureCookie;
    // HttpOnly 默认开启，避免前端脚本读取 BFF session id。
    private boolean httpOnlyCookie = true;
    private String sameSite = "Lax";
    // 临时授权请求的有效期；过期后 callback 会因为 state 缺失而失败。
    private Duration authorizationRequestTtl = Duration.ofMinutes(5);
    // Redis 中 BFF session 数据的有效期；浏览器 cookie 是会话级 cookie，关闭浏览器即丢失。
    private Duration redisSessionTtl = Duration.ofHours(12);
    // access token 过期前的提前刷新窗口。
    private Duration refreshSkew = Duration.ofSeconds(60);
    // 反向代理部署时从 Forwarded / X-Forwarded-* 头解析外部访问域名。
    private boolean trustForwardedHeaders = true;
    // Gateway BFF 默认保护所有非公开路径；认证以服务端 BFF session 为准，而不是浏览器传来的 JWT。
    private List<String> protectedPathPrefixes = List.of("/");
    private List<String> publicPathPrefixes = List.of("/oauth/");
    private Redis redis = new Redis();
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

    public URI getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(URI userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
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

    public static class Redis {
        private String keyPrefix = "oauth-oidc-client";

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(5);
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
    }
}
