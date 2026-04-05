package com.example.ratelimiter;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SlidingWindowStrategy implements RateLimitStrategy {
    private final RateLimitStorage storage;

    public SlidingWindowStrategy(RateLimitStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isAllowed(String key, RateLimiterRule rule) {
        SlidingWindowState state = storage.getClientState(key);
        if (state == null) {
            state = new SlidingWindowState();
        }

        long now = System.currentTimeMillis();
        long windowStart = now - rule.windowSizeMs();
        ConcurrentLinkedQueue<Long> timestamps = state.getRequestTimestamps();

        while (!timestamps.isEmpty()) {
            Long oldest = timestamps.peek();
            if (oldest != null && oldest < windowStart) {
                timestamps.poll();
            } else {
                break;
            }
        }

        boolean allowed = false;
        if (timestamps.size() < rule.maxTokens()) {
            timestamps.offer(now);
            allowed = true;
        }

        storage.saveClientState(key, state);
        return allowed;
    }
}
