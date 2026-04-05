package com.example.ratelimiter;

public interface RateLimitStorage {
    <T> T getClientState(String key);
    <T> void saveClientState(String key, T state);
}
