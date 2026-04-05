package com.example.ratelimiter;

import java.util.Optional;

public class RateLimiterService {
    private final RuleProvider ruleProvider;
    private final RateLimitStrategy strategy;
    private final KeyResolver keyResolver;

    public RateLimiterService(RuleProvider ruleProvider, RateLimitStrategy strategy,
                              KeyResolver keyResolver) {
        this.ruleProvider = ruleProvider;
        this.strategy = strategy;
        this.keyResolver = keyResolver;
    }

    public RateLimitResponse checkAccess(RateLimitRequest request) {
        Optional<RateLimiterRule> ruleOpt = ruleProvider.getRuleFor(request.endpoint());
        if (ruleOpt.isEmpty()) {
            return new RateLimitResponse(true, 0, "OK");
        }

        RateLimiterRule rule = ruleOpt.get();
        String key = keyResolver.resolve(request);

        boolean allowed = strategy.isAllowed(key, rule);
        if (allowed) {
            return new RateLimitResponse(true, 0, "OK");
        }

        String message = "Rate limit exceeded for " + request.endpoint();
        return new RateLimitResponse(false, rule.windowSizeMs(), message);
    }
}
