# Spring Gateway OAuth2/OIDC Client Starter

**语言:** [English](README.md) | [简体中文](README.zh-CN.md)

一个面向 Spring Cloud Gateway BFF 架构的 OAuth2/OIDC Client Starter，用于把 OAuth2/OIDC PKCE 登录、token 管理、刷新、退出和 access token 透传从浏览器迁移到后端 Gateway。

本项目不是 OAuth Server、IAM 平台、用户中心、RBAC 系统，也不是前端 OAuth SDK。它是一个可复用的 Spring Boot Starter，职责边界是 OAuth2/OIDC BFF 架构中的 Gateway Client 运行时。

## 它解决什么问题

现代 SPA 应用经常会在浏览器业务代码里处理 OAuth2 细节：PKCE state、callback、access token、refresh token、token 存储和 `Authorization` 请求头。本项目把这些职责移动到 Spring Cloud Gateway。

浏览器只保存 HttpOnly/SameSite session cookie。Gateway 负责 OAuth2/OIDC Client 流程、服务端 token 存储、按需刷新 token，并向下游微服务透传 access token。

```text
Browser
  -> HttpOnly session cookie
  -> Spring Cloud Gateway BFF
  -> Authorization: Bearer access_token
  -> Spring Cloud microservices
```

## 为什么使用它

### 更安全

- access token、refresh token、ID token、client secret 和 PKCE `code_verifier` 不进入浏览器业务代码。
- 由 Gateway 执行 Authorization Code + PKCE。
- authorization request 和 BFF session 存储在 Redis 中。
- OAuth2 `state` 在 callback 处理中只消费一次。
- 使用 HttpOnly/SameSite cookie 绑定浏览器会话。
- 只向下游服务透传 access token。
- refresh token 保持在 Gateway 服务端 session 中。
- 业务服务继续作为标准 Spring Security Resource Server。

### 更容易接入

- 提供可复用的 `oauth-oidc-client-starter`，避免每个 Gateway 服务重复复制 OAuth2 胶水代码。
- Gateway 只关注路由和配置。
- 业务服务只关注 Resource Server token 校验和 API 授权。
- 前端保持简单：只调用 Gateway API，不手动处理 OAuth2 token。

### 更高开发效率

- 减少重复编写 Spring Security、Spring Cloud Gateway、session、callback、refresh 和 token relay 配置。
- 提供本地四服务 demo，贴近目标生产架构。
- 认证边界更清晰：Gateway 处理登录、session、token 生命周期；业务服务验证 access token。

### 运行与运维收益

- 在 Gateway 层集中处理 token refresh 和 session 生命周期。
- 使用 Redis 存储 session 数据，比本地内存更适合多实例 Gateway 部署。
- 降低浏览器侧 token 处理复杂度。
- 下游身份传递保持标准的 `Authorization: Bearer <access_token>` 契约。

## 当前项目结构

```text
frontend/
  React + Vite demo UI

backend/
  auth-service/
    本地 demo Authorization Server / OIDC Provider

  gateway-service/
    Spring Cloud Gateway BFF 应用

  oauth-oidc-client-starter/
    可复用的 Gateway 侧 OAuth2/OIDC BFF Runtime Spring Boot Starter

  business-service/
    下游 Spring Security Resource Server demo
```

预期依赖方向：

```text
gateway-service -> oauth-oidc-client-starter
business-service -> Spring Security Resource Server
frontend -> 只调用 Gateway API
```

## 当前能力

当前实现已经包括：

- 四模块后端结构。
- Gateway 侧 OAuth2/OIDC 登录入口。
- PKCE `code_verifier` / `code_challenge` 生成和 OIDC `nonce` 校验。
- Redis-backed authorization request 存储。
- Redis-backed BFF session 存储。
- callback 中一次性消费 `state`。
- 基于原始浏览器访问 Host 的动态 callback origin。
- 登录成功后跳转初始化页。
- Gateway 向下游 API 透传 access token。
- access token refresh。
- 基于 Redis 的分布式 refresh lock，支持多实例 Gateway。
- 配置 fail-fast 校验。
- starter Maven artifact 发布能力。
- 基础 logout。

仍在路线图中的能力：

- 基于 `issuer-uri` 的 issuer discovery。
- 从 BFF session 中拆分出来的独立 token store 抽象。
- route 级认证和 token relay 配置。
- `business-service` 中更完整的 audience、scope、role 授权示例。

## 如何使用 Starter

在 Spring Cloud Gateway 应用中引入该包。Gateway 仍是你的 BFF 宿主：负责路由和基础安全配置；starter 负责 OAuth2/OIDC PKCE 登录、callback、Redis-backed BFF session、refresh、logout 和 access token relay。

### 1. 添加依赖

Gradle Gateway 应用示例：

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/iamxiaozhuang/oauth-oidc-client-starter")
        credentials {
            username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation "io.github.oidcclient:oauth-oidc-client-starter:1.0.3"
    implementation "org.springframework.cloud:spring-cloud-starter-gateway-server-webflux"
    implementation "org.springframework.boot:spring-boot-starter-security"
}
```

GitHub Packages 下载 Maven package 需要认证。可以在 Gradle properties 中配置 `gpr.user` 和 `gpr.key`，也可以设置 `GITHUB_ACTOR` 和具备 `read:packages` 权限的 GitHub token。

如果要在本仓库中验证本地发布包，先把 starter 发布到 Maven Local：

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :oauth-oidc-client-starter:publishToMavenLocal --no-daemon
```

然后让 demo Gateway 消费已发布 artifact，而不是项目内依赖：

```powershell
.\gradlew.bat :gateway-service:clean :gateway-service:build -PusePublishedStarter=true --no-daemon
```

仓库默认构建仍然使用 `project(':oauth-oidc-client-starter')`，所以全新 clone 后不需要先发布 artifact 也能构建。

### 2. 配置 Gateway

添加 Redis、Gateway routes 和 `oauth-oidc-client` 配置：

```yaml
spring:
  main:
    web-application-type: reactive
  data:
    redis:
      url: redis://localhost:6379
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: business-service
              uri: http://localhost:8081
              predicates:
                - Path=/api/**

oauth-oidc-client:
  authorization-endpoint: ${OAUTH_OIDC_AUTHORIZATION_ENDPOINT:http://localhost:9000/oauth2/authorize}
  token-endpoint: ${OAUTH_OIDC_TOKEN_ENDPOINT:http://localhost:9000/oauth2/token}
  client-id: ${OAUTH_OIDC_CLIENT_ID:oauth-oidc-client}
  client-secret: ${OAUTH_OIDC_CLIENT_SECRET:oauth-oidc-client-secret}
  callback-path: /oauth/callback
  login-success-path: /auth/init-page
  logout-success-path: /logout
  target-param: target
  original-url-param: target
  allowed-redirect-hosts:
    - localhost:5173
  protected-path-prefixes:
    - /api/
  public-path-prefixes:
    - /oauth/
  secure-cookie: false
  same-site: Lax
  redis-session-ttl: 12h
```

当前版本注意事项：

- 需要显式配置 provider endpoint，`issuer-uri` discovery 仍在路线图中。
- `allowed-redirect-hosts` 只填写 `host[:port]`，不包含 scheme 或 path。
- `callback-path`、`login-success-path`、`logout-success-path` 以及 protected/public prefixes 必须是站内相对路径。
- 生产环境 HTTPS 下应使用 `secure-cookie: true`。`same-site: None` 也要求 `secure-cookie: true`。
- Authorization Server 必须注册所有浏览器侧 callback URI，例如本地 demo 的 `http://localhost:5173/oauth/callback`。

### 3. 让 Starter 负责 BFF Session

Gateway 应用中可以让 Spring Security 放行请求，再由 starter 的 Gateway `GlobalFilter` 判断是否需要 BFF session。这样 Gateway 不会把浏览器请求当作 JWT 请求来校验，因为浏览器只应该发送 HttpOnly session cookie。

```java
package com.example.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
class GatewaySecurityConfiguration {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/oauth/**").permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
}
```

未认证的受保护请求到达时，starter 返回 `401` 并带上 `X-Login-Path`。demo frontend 会根据这个 header 跳转到 `/oauth/login`。callback 成功后，starter 写入 HttpOnly session cookie，并重定向到 `login-success-path?target=...`。

### 4. 保持前端和服务契约清晰

前端代码只通过 cookie 调用 Gateway API：

```ts
await fetch('/api/current-user', {
  credentials: 'include',
  headers: { Accept: 'application/json' },
});
```

前端不得调用 token endpoint、存储 token 或添加 `Authorization` header。下游服务应保持为普通 Spring Security Resource Server，验证 Gateway 透传的 access token：

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

## 本地 Demo

### 端口

| 服务 | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Gateway | `http://localhost:8080` |
| Business service | `http://localhost:8081` |
| Auth service | `http://localhost:9000` |
| Redis | `localhost:6379` |

### Demo 用户

```text
username: user
password: password
```

### Demo OAuth2 Client

```text
client-id: oauth-oidc-client
client-secret: oauth-oidc-client-secret
redirect-uri: http://localhost:5173/oauth/callback
```

## 前置条件

- Java 21。
- Node.js 和 npm。
- Redis 运行在 `localhost:6379`。
- 使用 `backend/` 目录下的 Gradle Wrapper；不需要全局安装 Gradle。

## 构建

构建后端：

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat clean build --no-daemon
```

构建前端：

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\frontend
npm run build
```

## 本地运行与调试

先启动 Redis。例如 Redis 通过 WSL 可用时：

```powershell
wsl redis-server
```

然后在不同终端启动后端服务：

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :auth-service:bootRun
```

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :business-service:bootRun
```

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :gateway-service:bootRun
```

启动前端：

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\frontend
npm run dev
```

打开：

```text
http://localhost:5173/
```

预期本地流程：

1. 前端加载并调用 Gateway API。
2. Gateway 发现不存在 BFF session。
3. Gateway 启动 OAuth2/OIDC Authorization Code + PKCE 登录。
4. auth service 认证 demo 用户。
5. callback 回到 Gateway-facing frontend origin。
6. Gateway 使用服务端保存的 PKCE verifier 换取 token。
7. Gateway 将 session 和 token 存入 Redis。
8. 前端再次调用 Gateway API，只携带 session cookie。
9. Gateway 向 `business-service` 透传 `Authorization: Bearer <access_token>`。
10. `business-service` 作为 Resource Server 校验 token，并返回当前用户。

## 配置示例

本地 Gateway demo 配置位于 `backend/gateway-service/src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      url: redis://localhost:6379

oauth-oidc-client:
  authorization-endpoint: ${OAUTH_OIDC_AUTHORIZATION_ENDPOINT:http://localhost:9000/oauth2/authorize}
  token-endpoint: ${OAUTH_OIDC_TOKEN_ENDPOINT:http://localhost:9000/oauth2/token}
  client-id: ${OAUTH_OIDC_CLIENT_ID:oauth-oidc-client}
  client-secret: ${OAUTH_OIDC_CLIENT_SECRET:oauth-oidc-client-secret}
  callback-path: /oauth/callback
  login-success-path: /auth/init-page
  allowed-redirect-hosts:
    - ${OAUTH_OIDC_ALLOWED_REDIRECT_HOST:localhost:5173}
  protected-path-prefixes:
    - /api/
  public-path-prefixes:
    - /oauth/
  secure-cookie: false
  same-site: Lax
  redis-session-ttl: 12h
```

生产环境应使用安全的密钥、HTTPS、secure cookie、可信 forwarded headers、托管 Redis，以及企业内部 Authorization Server / OIDC Provider。

## 设计边界

这个 starter 不应该变成 Authorization Server、IAM 产品、RBAC 系统、租户权限平台或外部身份源适配器。Google、Azure AD、LDAP、SAML、CAS 或企业 SSO 等外部提供方应接入内部 Authorization Server / OIDC Provider，而不是直接接入本 starter。

starter 的职责是 Gateway BFF Client Runtime：

- 发起登录。
- 处理 callback。
- 管理服务端 session 和 token。
- 刷新 token。
- 退出登录。
- 向下游服务透传 access token。

业务服务仍然负责使用 Spring Security Resource Server 校验 access token，并执行 API 授权。
