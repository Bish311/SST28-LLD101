package com.example.ratelimiter;

public record RateLimitRequest(String clientId, String endpoint, long timestamp) {
}
