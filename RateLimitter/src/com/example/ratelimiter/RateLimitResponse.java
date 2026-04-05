package com.example.ratelimiter;

public record RateLimitResponse(boolean isAllowed, long retryAfterMs, String message) {
}
