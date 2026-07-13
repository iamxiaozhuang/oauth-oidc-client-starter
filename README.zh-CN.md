# Spring Gateway OAuth2/OIDC Client Starter

**语言：** [English](README.md) | [简体中文](README.zh-CN.md)

一个让 Spring Cloud Gateway 具备 OAuth2/OIDC BFF 能力的 Spring Boot Starter。它统一处理 Authorization Code + PKCE 登录、服务端会话、token 刷新、退出登录和 access token 透传，让前端只需要使用 HttpOnly Cookie。

```text
Browser -- HttpOnly Cookie --> Spring Cloud Gateway BFF
                                  |
                                  +-- Bearer access token --> Resource Server
```

## 核心优势

- access token、refresh token、ID token、client secret 和 PKCE verifier 全部保留在服务端。
- authorization request 和 BFF session 使用 Redis 存储。
- 校验 OIDC `state` 与 `nonce`，callback 只能消费一次。
- 使用 Redis 分布式锁协调 access token 刷新。
- 通过标准 `Authorization: Bearer` 请求头向下游服务传递身份。
- 以 Starter 方式接入，并提供启动时配置校验。
- 支持经过白名单校验的动态 callback 域名。

## 项目结构

```text
frontend/                              React + Vite 示例前端
backend/
  auth-service/                        本地 OIDC Provider
  gateway-service/                     Spring Cloud Gateway BFF 示例
  oauth-oidc-client-starter/           可复用 Starter
  business-service/                    Resource Server 示例
```

## 标准使用方式

### 1. 添加依赖

Artifact 发布在 GitHub Packages，下载时需要具备 `read:packages` 权限的 GitHub Token。

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

### 2. 配置 Redis、路由和 OIDC

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
  authorization-endpoint: https://id.example.com/oauth2/authorize
  token-endpoint: https://id.example.com/oauth2/token
  client-id: gateway-client
  client-secret: ${OAUTH_OIDC_CLIENT_SECRET}
  callback-path: /oauth/callback
  login-success-path: /auth/init-page
  logout-success-path: /logout
  allowed-redirect-hosts:
    - app.example.com
  protected-path-prefixes:
    - /api/
  public-path-prefixes:
    - /oauth/
  secure-cookie: true
  same-site: Lax
  redis-session-ttl: 12h
```

在 OIDC Provider 中注册 `https://app.example.com/oauth/callback`。当前版本使用显式配置的 authorization endpoint 和 token endpoint。

### 3. 按 BFF 契约调用

Gateway 的 Spring Security 放行请求，由 Starter Filter 对受保护路径校验 BFF session：

```java
@Bean
SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
            .build();
}
```

前端通过 Cookie 调用 Gateway API：

```ts
await fetch('/api/current-user', { credentials: 'include' });
```

下游服务继续使用标准 Spring Security Resource Server，验证 Gateway 透传的 access token。

## 本地运行

环境要求：Java 21、Node.js、npm，以及运行在 `localhost:6379` 的 Redis。

构建项目：

```powershell
cd backend
.\gradlew.bat clean build --no-daemon

cd ..\frontend
npm install
npm run build
```

启动 Redis，然后在不同终端启动三个后端服务：

```powershell
cd backend
.\gradlew.bat :auth-service:bootRun
.\gradlew.bat :business-service:bootRun
.\gradlew.bat :gateway-service:bootRun
```

启动前端：

```powershell
cd frontend
npm run dev
```

打开 `http://localhost:5173`，使用 `user` / `password` 登录。

| 服务 | 地址 |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Gateway | `http://localhost:8080` |
| Business service | `http://localhost:8081` |
| Auth service | `http://localhost:9000` |
| Redis | `localhost:6379` |

更多后端细节见 [backend/README.md](backend/README.md)。
