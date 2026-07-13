package io.github.oidcclient.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthOidcClientClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void tokenExchangeAuthenticatesConfidentialClientWithBasicHeader() throws Exception {
        CapturedRequest captured = new CapturedRequest();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oauth2/token", exchange -> {
            captured.authorization = exchange.getRequestHeaders().getFirst("Authorization");
            captured.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respondJson(exchange, 200, "{\"access_token\":\"access-token\",\"token_type\":\"Bearer\",\"expires_in\":600}");
        });
        server.start();

        OAuthOidcClientClient client = new OAuthOidcClientClient(config(tokenEndpoint()), java.net.http.HttpClient.newHttpClient(), Duration.ofSeconds(2));

        client.exchangeCode("code-123", "verifier-123", URI.create("http://app.example.test/oauth/callback"));

        assertThat(captured.authorization).isEqualTo(basic("oauth-oidc-client", "oauth-oidc-client-secret"));
        assertThat(captured.body).contains("client_id=oauth-oidc-client");
        assertThat(captured.body).doesNotContain("client_secret");
        assertThat(captured.body).contains("code_verifier=verifier-123");
        assertThat(captured.body).contains("redirect_uri=http%3A%2F%2Fapp.example.test%2Foauth%2Fcallback");
    }

    @Test
    void authorizationRequestStoresNonceAndSendsItToProvider() {
        OAuthOidcClientClient client = new OAuthOidcClientClient(config(URI.create("http://localhost:9000/oauth2/token")));

        AuthorizationRequest request = client.createAuthorizationRequest(
                URI.create("https://app.example.test/oauth/callback"),
                URI.create("https://app.example.test"),
                "/dashboard",
                URI.create("https://app.example.test/login-page")
        );

        assertThat(request.nonce()).isNotBlank();
        assertThat(request.authorizationUri().toString()).contains("nonce=" + request.nonce());
    }

    private URI tokenEndpoint() {
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/oauth2/token");
    }

    private static OAuthOidcClientConfig config(URI tokenEndpoint) {
        return OAuthOidcClientConfig.builder()
                .authorizationEndpoint(URI.create("http://localhost:9000/oauth2/authorize"))
                .tokenEndpoint(tokenEndpoint)
                .clientId("oauth-oidc-client")
                .clientSecret("oauth-oidc-client-secret")
                .build();
    }

    private static String basic(String clientId, String clientSecret) {
        return "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class CapturedRequest {
        private String authorization;
        private String body;
    }
}
