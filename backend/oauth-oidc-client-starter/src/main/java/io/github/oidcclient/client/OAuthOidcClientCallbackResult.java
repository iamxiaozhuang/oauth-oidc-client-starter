package io.github.oidcclient.client;

import java.net.URI;

public final class OAuthOidcClientCallbackResult {
    private final BffSession session;
    private final URI initRedirectUri;

    public OAuthOidcClientCallbackResult(BffSession session, URI initRedirectUri) {
        this.session = session;
        this.initRedirectUri = initRedirectUri;
    }

    public BffSession session() {
        return session;
    }

    public URI initRedirectUri() {
        return initRedirectUri;
    }
}
