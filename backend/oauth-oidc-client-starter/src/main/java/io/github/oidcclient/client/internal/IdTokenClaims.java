package io.github.oidcclient.client.internal;

import io.github.oidcclient.client.OAuthOidcClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class IdTokenClaims {
    private IdTokenClaims() {
    }

    public static Map<String, Object> parsePayload(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new OAuthOidcClientException("token response does not contain id_token");
        }
        String[] parts = idToken.split("\\.", -1);
        if (parts.length != 3 || parts[1].isBlank()) {
            throw new OAuthOidcClientException("id_token is not a compact JWT");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return Json.parseObject(new String(payload, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            throw new OAuthOidcClientException("failed to parse id_token claims", ex);
        }
    }
}
