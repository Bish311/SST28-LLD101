package com.example.ratelimiter;

import java.util.Optional;

public interface RuleProvider {
    Optional<RateLimiterRule> getRuleFor(String endpoint);
}
