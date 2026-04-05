package com.example.ratelimiter;

public class TokenBucketStrategy implements RateLimitStrategy {
    private final RateLimitStorage storage;

    public TokenBucketStrategy(RateLimitStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isAllowed(String key, RateLimiterRule rule) {
        TokenBucketState state = storage.getClientState(key);
        if (state == null) {
            state = new TokenBucketState(rule.maxTokens());
        }

        long now = System.currentTimeMillis();
        long lastRefill = state.getLastRefillTimestamp().get();
        long elapsedTime = now - lastRefill;

        int tokensToAdd = (int) ((elapsedTime / (double) rule.windowSizeMs()) * rule.maxTokens());
        if (tokensToAdd > 0) {
            state.getLastRefillTimestamp().set(now);
            int current = state.getCurrentTokens().get();
            int newTokens = Math.min(rule.maxTokens(), current + tokensToAdd);
            state.getCurrentTokens().set(newTokens);
        }

        boolean allowed = false;
        if (state.getCurrentTokens().get() > 0) {
            state.getCurrentTokens().decrementAndGet();
            allowed = true;
        }

        storage.saveClientState(key, state);
        return allowed;
    }
}
