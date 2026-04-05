package com.example.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketState {
    private final AtomicInteger currentTokens;
    private final AtomicLong lastRefillTimestamp;

    public TokenBucketState(int maxTokens) {
        this.currentTokens = new AtomicInteger(maxTokens);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public AtomicInteger getCurrentTokens() {
        return currentTokens;
    }

    public AtomicLong getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }
}
