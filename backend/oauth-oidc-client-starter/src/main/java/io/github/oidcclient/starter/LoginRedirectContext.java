package io.github.oidcclient.starter;

import java.net.URI;

final class LoginRedirectContext {
    private final URI originalOrigin;
    private final String originalPath;
    private final URI redirectUri;
    private final URI initPageUri;

    LoginRedirectContext(URI originalOrigin, String originalPath, URI redirectUri, URI initPageUri) {
        this.originalOrigin = originalOrigin;
        this.originalPath = originalPath;
        this.redirectUri = redirectUri;
        this.initPageUri = initPageUri;
    }

    URI originalOrigin() {
        return originalOrigin;
    }

    String originalPath() {
        return originalPath;
    }

    URI redirectUri() {
        return redirectUri;
    }

    URI initPageUri() {
        return initPageUri;
    }
}
