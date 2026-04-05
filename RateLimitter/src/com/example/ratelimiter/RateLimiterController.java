package com.example.ratelimiter;

public class RateLimiterController {
    private final RateLimiterService service;

    public RateLimiterController(RateLimiterService service) {
        this.service = service;
    }

    public RateLimitResponse checkAccess(RateLimitRequest request) {
        return service.checkAccess(request);
    }
}
