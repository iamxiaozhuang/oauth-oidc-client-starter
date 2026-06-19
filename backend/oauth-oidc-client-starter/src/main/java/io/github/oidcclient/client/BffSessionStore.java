package io.github.oidcclient.client;

import java.util.Optional;

public interface BffSessionStore {
    void save(BffSession session);

    Optional<BffSession> find(String sessionId);

    void delete(String sessionId);
}
