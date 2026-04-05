package com.example.ratelimiter;

public interface KeyResolver {
    String resolve(RateLimitRequest request);
}
