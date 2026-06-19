package io.github.oidcclient.client.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pkce {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Pkce() {
    }

    public static String randomUrlSafe(int byteLength) {
        // 使用 URL-safe Base64 且去掉 padding，生成可直接放进 OAuth 参数和 cookie 的随机值。
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String challengeS256(String codeVerifier) {
        try {
            // PKCE S256: code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
