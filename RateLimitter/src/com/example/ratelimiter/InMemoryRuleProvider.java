package com.example.ratelimiter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRuleProvider implements RuleProvider {
    private final ConcurrentHashMap<String, RateLimiterRule> rules;

    public InMemoryRuleProvider() {
        this.rules = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<RateLimiterRule> getRuleFor(String endpoint) {
        return Optional.ofNullable(rules.get(endpoint));
    }

    public void addRule(RateLimiterRule rule) {
        rules.put(rule.endpoint(), rule);
    }

    public void removeRule(String ruleId) {
        for (String key : rules.keySet()) {
            RateLimiterRule rule = rules.get(key);
            if (rule != null && rule.ruleId().equals(ruleId)) {
                rules.remove(key);
                return;
            }
        }
    }
}
