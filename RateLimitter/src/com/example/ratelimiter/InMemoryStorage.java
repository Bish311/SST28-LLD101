package com.example.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorage implements RateLimitStorage {
    private final ConcurrentHashMap<String, Object> stateMap;

    public InMemoryStorage() {
        this.stateMap = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClientState(String key) {
        return (T) stateMap.get(key);
    }

    @Override
    public <T> void saveClientState(String key, T state) {
        stateMap.put(key, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateClientState(String key, T defaultState) {
        Object existing = stateMap.putIfAbsent(key, defaultState);
        if (existing != null) {
            return (T) existing;
        }
        return defaultState;
    }
}
