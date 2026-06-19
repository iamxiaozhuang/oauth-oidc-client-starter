package io.github.oidcclient.client;

public class OAuthOidcClientException extends RuntimeException {
    public OAuthOidcClientException(String message) {
        super(message);
    }

    public OAuthOidcClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
