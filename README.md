# Spring Gateway OAuth2/OIDC Client Starter

A Spring Cloud Gateway BFF starter that moves OAuth2/OIDC PKCE login, token management, refresh, logout, and access-token relay from the browser into the backend Gateway.

This project is not an OAuth server, IAM platform, user center, RBAC system, or frontend OAuth SDK. It is a reusable Spring Boot starter for the Gateway side of an OAuth2/OIDC BFF architecture.

## What It Does

Modern SPA applications often end up handling OAuth2 details in browser code: PKCE state, callback handling, access tokens, refresh tokens, token storage, and `Authorization` headers. This project moves that responsibility to Spring Cloud Gateway.

The browser keeps only an HttpOnly/SameSite session cookie. The Gateway owns the OAuth2/OIDC client flow, stores tokens on the server side, refreshes tokens when needed, and relays the access token to downstream microservices.

```text
Browser
  -> HttpOnly session cookie
  -> Spring Cloud Gateway BFF
  -> Authorization: Bearer access_token
  -> Spring Cloud microservices
```

## Why Use It

### Better Security

- Keeps access tokens, refresh tokens, ID tokens, client secrets, and PKCE `code_verifier` out of browser business code.
- Uses Authorization Code + PKCE from the Gateway.
- Stores authorization requests and BFF sessions in Redis.
- Consumes OAuth2 `state` only once during callback handling.
- Uses HttpOnly/SameSite cookies for browser session binding.
- Relays only the access token to downstream services.
- Keeps refresh tokens inside the Gateway-side server session.
- Lets business services remain standard Spring Security Resource Servers.

### Easier Integration

- Provides a reusable `oauth-oidc-client-starter` module instead of copying OAuth2 glue code into every Gateway service.
- Keeps the Gateway focused on routing and configuration.
- Keeps business services focused on Resource Server validation and API authorization.
- Keeps the frontend simple: it calls Gateway APIs and never manually handles OAuth2 tokens.

### Higher Development Efficiency

- Reduces repeated Spring Security, Spring Cloud Gateway, session, callback, refresh, and token-relay wiring.
- Gives teams a local four-service demo that mirrors the intended production architecture.
- Makes the authentication boundary clear: Gateway handles login/session/token lifecycle; business services validate access tokens.

### Runtime And Operational Benefits

- Centralizes token refresh and session lifecycle in the Gateway layer.
- Uses Redis-backed session data, which is a better fit for multi-instance Gateway deployments than local memory.
- Avoids unnecessary token handling in the browser and reduces frontend authentication complexity.
- Keeps downstream identity propagation on the standard `Authorization: Bearer <access_token>` contract.

## Current Project Structure

```text
frontend/
  React + Vite demo UI

backend/
  auth-service/
    Local demo Authorization Server / OIDC Provider

  gateway-service/
    Spring Cloud Gateway BFF application

  oauth-oidc-client-starter/
    Reusable Spring Boot starter for Gateway-side OAuth2/OIDC BFF runtime

  business-service/
    Downstream Spring Security Resource Server demo
```

The intended dependency direction is:

```text
gateway-service -> oauth-oidc-client-starter
business-service -> Spring Security Resource Server
frontend -> Gateway APIs only
```

## Current Capabilities

The current implementation includes:

- Four-module backend structure.
- Gateway-side OAuth2/OIDC login entry.
- PKCE `code_verifier` / `code_challenge` generation and OIDC `nonce` validation.
- Redis-backed authorization request storage.
- Redis-backed BFF session storage.
- One-time `state` consumption during callback.
- Dynamic callback origin based on the original browser-facing host.
- Login success redirect to an initialization page.
- Gateway access-token relay to downstream APIs.
- Access-token refresh.
- Redis-backed distributed refresh locking for multi-instance Gateway deployments.
- Fail-fast configuration validation.
- Maven artifact publication for starter-style usage.
- Basic logout.

Known gaps still on the roadmap:

- Issuer discovery from `issuer-uri`.
- Dedicated token store abstraction separate from the BFF session.
- Route-level authentication and token-relay configuration.
- Additional audience, scope, and role authorization examples in `business-service`.

## Starter Dependency

Use the starter from a Gateway application as a normal Maven dependency:

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
    implementation "io.github.oidcclient:oauth-oidc-client-starter:1.0.1"
}
```

GitHub Packages requires authentication for Maven package downloads. Configure
`gpr.user` and `gpr.key` in your Gradle properties, or set `GITHUB_ACTOR` and a
GitHub token with `read:packages` access in the environment.

When validating this repository against a locally published starter artifact,
run:

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :oauth-oidc-client-starter:publishToMavenLocal --no-daemon
.\gradlew.bat clean build -PusePublishedStarter=true --no-daemon
```

## Local Demo

### Ports

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Gateway | `http://localhost:8080` |
| Business service | `http://localhost:8081` |
| Auth service | `http://localhost:9000` |
| Redis | `localhost:6379` |

### Demo Credentials

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

## Prerequisites

- Java 21.
- Node.js and npm.
- Redis running on `localhost:6379`.
- Use the Gradle Wrapper from `backend/`; a global Gradle installation is not required.

## Build

Build the backend:

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat clean build --no-daemon
```

Build the frontend:

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\frontend
npm run build
```

## Run And Debug Locally

Start Redis first. For example, if Redis is available through WSL:

```powershell
wsl redis-server
```

Then start the backend services in separate terminals:

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

Start the frontend:

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\frontend
npm run dev
```

Open:

```text
http://localhost:5173/
```

The expected local flow is:

1. The frontend loads and calls the Gateway API.
2. The Gateway sees that no BFF session exists.
3. The Gateway starts OAuth2/OIDC Authorization Code + PKCE login.
4. The auth service authenticates the demo user.
5. The callback returns to the Gateway-facing frontend origin.
6. The Gateway exchanges the code using the server-side PKCE verifier.
7. The Gateway stores the session and tokens in Redis.
8. The frontend calls the Gateway API again with only the session cookie.
9. The Gateway relays `Authorization: Bearer <access_token>` to `business-service`.
10. `business-service` validates the token as a Resource Server and returns the current user.

## Configuration Example

The local Gateway demo is configured in `backend/gateway-service/src/main/resources/application.yml`:

```yaml
spring:
  data:
    redis:
      url: redis://localhost:6379

oauth-oidc-client:
  authorization-endpoint: http://localhost:9000/oauth2/authorize
  token-endpoint: http://localhost:9000/oauth2/token
  user-info-endpoint: http://localhost:9000/userinfo
  client-id: oauth-oidc-client
  client-secret: oauth-oidc-client-secret
  callback-path: /oauth/callback
  login-success-path: /auth/init-page
  allowed-redirect-hosts:
    - localhost:5173
  secure-cookie: false
  same-site: Lax
  redis-session-ttl: 12h
```

In production, use secure secrets, HTTPS, secure cookies, trusted forwarded headers, a managed Redis deployment, and an enterprise Authorization Server / OIDC Provider.

## Design Boundary

This starter should not become an Authorization Server, IAM product, RBAC system, tenant permission platform, or external identity-source adapter. External providers such as Google, Azure AD, LDAP, SAML, CAS, or enterprise SSO systems should connect to the internal Authorization Server / OIDC Provider, not directly into this starter.

The starter's job is the Gateway BFF client runtime:

- Start login.
- Handle callback.
- Manage server-side session and tokens.
- Refresh tokens.
- Logout.
- Relay access tokens to downstream services.

Business services remain responsible for validating access tokens and enforcing API authorization with Spring Security Resource Server.
