package io.github.oidcclient.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class InMemoryRefreshTokenLock implements RefreshTokenLock {
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public LockHandle acquire(String sessionId) {
        ReentrantLock lock = locks.computeIfAbsent(sessionId, ignored -> new ReentrantLock());
        lock.lock();
        return () -> {
            try {
                lock.unlock();
            } finally {
                if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                    locks.remove(sessionId, lock);
                }
            }
        };
    }
}
