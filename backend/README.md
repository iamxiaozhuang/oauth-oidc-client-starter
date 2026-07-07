# OAuth2/OIDC Client Starter Backend

This backend is being refactored into a Spring Cloud Gateway BFF authentication demo with four modules:

- `auth-service`: internal OAuth2/OIDC authorization service for local demos.
- `oauth-oidc-client-starter`: reusable Spring Boot starter that owns Gateway-side OAuth2/OIDC PKCE login, BFF session, token lifecycle, and token relay.
- `gateway-service`: Spring Cloud Gateway BFF service that depends on the starter.
- `business-service`: downstream Spring Security Resource Server demo.

Target runtime flow:

```text
Browser
  -> Cookie
  -> gateway-service
  -> Authorization: Bearer access_token
  -> business-service
```

The browser must not fetch or store tokens. It calls the Gateway API only. The Gateway owns login, callback handling, server-side token storage, refresh, logout, and access-token relay.

## Local Demo Ports

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8080`
- Business service: `http://localhost:8081`
- Auth service: `http://localhost:9000`

Demo auth-service user:

```text
username: user
password: password
```

Demo OAuth2 client used by the Gateway:

```text
client-id: oauth-oidc-client
client-secret: oauth-oidc-client-secret
redirect-uri: http://localhost:5173/oauth/callback
```

The starter builds the authorization request `redirect_uri` from the browser
domain that starts login. In the local demo, `localhost:5173` is treated as an
allowed browser-facing domain through `oauth-oidc-client.allowed-redirect-hosts`.
The generated `state` entry stored in Redis records `originalOrigin`,
`originalPath`, redirect URI, init page URI, and PKCE verifier. The callback
must arrive on the saved origin and redirect URI before the code is
exchanged. After callback, the browser is redirected to
`originalOrigin + login-success-path?target=originalPath`.

Access-token refresh is guarded by a Redis refresh lock keyed by BFF session
id, so multiple Gateway instances do not use the same refresh token
concurrently. The demo exposes the lock timing through
`oauth-oidc-client.refresh-lock-ttl`, `refresh-lock-wait-timeout`, and
`refresh-lock-retry-interval`.

The demo authorization server registers those client values with Spring Boot's
standard `spring.security.oauth2.authorizationserver.client.*` properties. In a
real deployment, the starter user configures only the Gateway side; the
enterprise Authorization Server owns its client registration separately.

## Build

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat clean build --no-daemon
```

## Starter Dependency Usage

Applications consume the starter as a normal Maven artifact:

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

For local development, publish the starter to Maven Local first:

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat :oauth-oidc-client-starter:publishToMavenLocal --no-daemon
.\gradlew.bat clean build -PusePublishedStarter=true --no-daemon
```

The repository's default build still uses the included starter project so a
fresh clone can build without first publishing the artifact.

## Run Locally

Use separate terminals:

```powershell
.\gradlew.bat :auth-service:bootRun
.\gradlew.bat :business-service:bootRun
.\gradlew.bat :gateway-service:bootRun
```

Then start the frontend from `..\frontend`:

```powershell
npm run dev
```
