package com.example.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SlidingWindowCounterState {
    private final AtomicInteger previousWindowCount;
    private final AtomicInteger currentWindowCount;
    private final AtomicLong currentWindowStart;

    public SlidingWindowCounterState() {
        this.previousWindowCount = new AtomicInteger(0);
        this.currentWindowCount = new AtomicInteger(0);
        this.currentWindowStart = new AtomicLong(System.currentTimeMillis());
    }

    public AtomicInteger getPreviousWindowCount() {
        return previousWindowCount;
    }

    public AtomicInteger getCurrentWindowCount() {
        return currentWindowCount;
    }

    public AtomicLong getCurrentWindowStart() {
        return currentWindowStart;
    }
}
