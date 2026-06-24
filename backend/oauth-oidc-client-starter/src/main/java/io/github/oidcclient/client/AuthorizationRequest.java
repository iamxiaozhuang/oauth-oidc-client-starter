package io.github.oidcclient.client;

import java.net.URI;

public final class AuthorizationRequest {
    private final URI authorizationUri;
    private final URI redirectUri;
    private final String state;
    private final String nonce;
    private final String codeVerifier;
    private final String codeChallenge;
    private final URI originalOrigin;
    private final String originalPath;
    private final URI initPageUri;

    public AuthorizationRequest(
            URI authorizationUri,
            URI redirectUri,
            String state,
            String nonce,
            String codeVerifier,
            String codeChallenge,
            URI originalOrigin,
            String originalPath,
            URI initPageUri
    ) {
        this.authorizationUri = authorizationUri;
        this.redirectUri = redirectUri;
        this.state = state;
        this.nonce = nonce;
        this.codeVerifier = codeVerifier;
        this.codeChallenge = codeChallenge;
        this.originalOrigin = originalOrigin;
        this.originalPath = originalPath;
        this.initPageUri = initPageUri;
    }

    public URI authorizationUri() {
        return authorizationUri;
    }

    public URI redirectUri() {
        return redirectUri;
    }

    public String state() {
        return state;
    }

    public String nonce() {
        return nonce;
    }

    public String codeVerifier() {
        return codeVerifier;
    }

    public String codeChallenge() {
        return codeChallenge;
    }

    public URI originalOrigin() {
        return originalOrigin;
    }

    public String originalPath() {
        return originalPath;
    }

    public URI initPageUri() {
        return initPageUri;
    }
}
