package com.example.ratelimiter;

public class RateLimitManagementController {
    private final InMemoryRuleProvider ruleProvider;

    public RateLimitManagementController(InMemoryRuleProvider ruleProvider) {
        this.ruleProvider = ruleProvider;
    }

    public void addRule(RateLimiterRule rule) {
        ruleProvider.addRule(rule);
    }

    public void removeRule(String ruleId) {
        ruleProvider.removeRule(ruleId);
    }
}
