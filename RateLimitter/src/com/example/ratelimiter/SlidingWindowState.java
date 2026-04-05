package com.example.ratelimiter;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SlidingWindowState {
    private final ConcurrentLinkedQueue<Long> requestTimestamps;

    public SlidingWindowState() {
        this.requestTimestamps = new ConcurrentLinkedQueue<>();
    }

    public ConcurrentLinkedQueue<Long> getRequestTimestamps() {
        return requestTimestamps;
    }
}
