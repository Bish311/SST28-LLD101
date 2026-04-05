package com.example.ratelimiter;

public record RateLimiterRule(String ruleId, String endpoint, int maxTokens, long windowSizeMs) {
}
