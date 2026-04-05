package com.example.ratelimiter;

public interface RateLimitStrategy {
    boolean isAllowed(String key, RateLimiterRule rule);
}
