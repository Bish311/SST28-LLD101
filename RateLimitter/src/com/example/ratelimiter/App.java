package com.example.ratelimiter;

public class App {
    public static void main(String[] args) {
        runTokenBucketDemo();
        System.out.println();
        runSlidingWindowDemo();
    }

    private static void runTokenBucketDemo() {
        System.out.println("=== Rate Limiter Demo (Token Bucket) ===");
        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        
        RateLimiterRule rule = new RateLimiterRule("rule-1", "/api/search", 5, 10000);
        ruleProvider.addRule(rule);

        TokenBucketStrategy strategy = new TokenBucketStrategy(storage);
        RateLimiterService service = new RateLimiterService(ruleProvider, strategy);
        RateLimiterController controller = new RateLimiterController(service);

        for (int i = 1; i <= 6; i++) {
            RateLimitRequest request = new RateLimitRequest("Bishwayan", "/api/search", System.currentTimeMillis());
            RateLimitResponse response = controller.checkAccess(request);
            printResponse(i, response);
        }
    }

    private static void runSlidingWindowDemo() {
        System.out.println("=== Rate Limiter Demo (Sliding Window) ===");
        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        
        RateLimiterRule rule = new RateLimiterRule("rule-2", "/api/search", 5, 10000);
        ruleProvider.addRule(rule);

        SlidingWindowStrategy strategy = new SlidingWindowStrategy(storage);
        RateLimiterService service = new RateLimiterService(ruleProvider, strategy);
        RateLimiterController controller = new RateLimiterController(service);

        for (int i = 1; i <= 6; i++) {
            RateLimitRequest request = new RateLimitRequest("Bish", "/api/search", System.currentTimeMillis());
            RateLimitResponse response = controller.checkAccess(request);
            printResponse(i, response);
        }
    }

    private static void printResponse(int requestNumber, RateLimitResponse response) {
        if (response.isAllowed()) {
            System.out.println("Request " + requestNumber + ": ALLOWED (retryAfter=0ms)");
        } else {
            System.out.println("Request " + requestNumber + ": REJECTED (retryAfter=" + response.retryAfterMs() + "ms) \u2014 " + response.message());
        }
    }
}
