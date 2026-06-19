package io.github.oidcclient.client;

import java.util.Optional;

public interface AuthorizationRequestStore {
    void save(AuthorizationRequest request);

    Optional<AuthorizationRequest> remove(String state);
}
