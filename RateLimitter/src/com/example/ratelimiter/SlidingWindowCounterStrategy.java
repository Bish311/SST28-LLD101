package com.example.ratelimiter;

public class SlidingWindowCounterStrategy implements RateLimitStrategy {
    private final RateLimitStorage storage;

    public SlidingWindowCounterStrategy(RateLimitStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isAllowed(String key, RateLimiterRule rule) {
        SlidingWindowCounterState state = storage.getOrCreateClientState(key,
                new SlidingWindowCounterState());

        long now = System.currentTimeMillis();
        long windowStart = state.getCurrentWindowStart().get();
        long elapsed = now - windowStart;

        if (elapsed >= rule.windowSizeMs()) {
            long windowsSkipped = elapsed / rule.windowSizeMs();
            if (windowsSkipped == 1) {
                state.getPreviousWindowCount().set(state.getCurrentWindowCount().get());
            } else {
                state.getPreviousWindowCount().set(0);
            }
            state.getCurrentWindowCount().set(0);
            state.getCurrentWindowStart().set(windowStart + (windowsSkipped * rule.windowSizeMs()));
        }

        long currentWindowStart = state.getCurrentWindowStart().get();
        long positionInWindow = now - currentWindowStart;
        double previousWeight = 1.0 - ((double) positionInWindow / rule.windowSizeMs());
        if (previousWeight < 0) {
            previousWeight = 0;
        }

        double weightedCount = (state.getPreviousWindowCount().get() * previousWeight)
                + state.getCurrentWindowCount().get();

        if (weightedCount < rule.maxTokens()) {
            state.getCurrentWindowCount().incrementAndGet();
            return true;
        }

        return false;
    }
}
