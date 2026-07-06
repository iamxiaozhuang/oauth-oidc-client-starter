package io.github.oidcclient.client;

public interface RefreshTokenLock {
    LockHandle acquire(String sessionId);

    interface LockHandle extends AutoCloseable {
        @Override
        void close();
    }
}
