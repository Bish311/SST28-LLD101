package com.example.ratelimiter;

public class FixedWindowStrategy implements RateLimitStrategy {
    private final RateLimitStorage storage;

    public FixedWindowStrategy(RateLimitStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isAllowed(String key, RateLimiterRule rule) {
        FixedWindowState state = storage.getOrCreateClientState(key, new FixedWindowState());

        long now = System.currentTimeMillis();
        long windowStart = state.getWindowStartTimestamp().get();
        long elapsed = now - windowStart;

        if (elapsed >= rule.windowSizeMs()) {
            if (state.getWindowStartTimestamp().compareAndSet(windowStart, now)) {
                state.getRequestCount().set(0);
            }
        }

        while (true) {
            int current = state.getRequestCount().get();
            if (current >= rule.maxTokens()) {
                return false;
            }
            if (state.getRequestCount().compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }
}
