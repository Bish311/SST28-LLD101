package com.example.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FixedWindowState {
    private final AtomicInteger requestCount;
    private final AtomicLong windowStartTimestamp;

    public FixedWindowState() {
        this.requestCount = new AtomicInteger(0);
        this.windowStartTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public AtomicInteger getRequestCount() {
        return requestCount;
    }

    public AtomicLong getWindowStartTimestamp() {
        return windowStartTimestamp;
    }
}
