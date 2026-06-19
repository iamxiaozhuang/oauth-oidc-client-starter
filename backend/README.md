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

The demo authorization server registers those client values with Spring Boot's
standard `spring.security.oauth2.authorizationserver.client.*` properties. In a
real deployment, the starter user configures only the Gateway side; the
enterprise Authorization Server owns its client registration separately.

## Build

```powershell
cd D:\CodexProjects\spring-gateway-oidc-client-starter\backend
.\gradlew.bat clean build --no-daemon
```

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
