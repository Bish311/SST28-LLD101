package com.example.ratelimiter;

public class App {
    private static final int REQUESTS_PER_WINDOW = 5;
    private static final long WINDOW_SIZE_MS = 60000;

    public static void main(String[] args) {
        runFixedWindowDemo();
        System.out.println();
        runSlidingWindowCounterDemo();
        System.out.println();
        runTokenBucketDemo();
        System.out.println();
        runExternalResourceFlowDemo();
    }

    private static void runFixedWindowDemo() {
        System.out.println("=== Rate Limiter Demo (Fixed Window Counter) ===");
        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        ruleProvider.addRule(new RateLimiterRule("rule-fw", "/external/paid-api",
                REQUESTS_PER_WINDOW, WINDOW_SIZE_MS));

        FixedWindowStrategy strategy = new FixedWindowStrategy(storage);
        RateLimiterService service = new RateLimiterService(ruleProvider, strategy,
                new ClientIdKeyResolver());
        RateLimiterController controller = new RateLimiterController(service);

        for (int i = 1; i <= 7; i++) {
            RateLimitRequest request = new RateLimitRequest("Bish",
                    "/external/paid-api", System.currentTimeMillis());
            RateLimitResponse response = controller.checkAccess(request);
            printResponse(i, response);
        }
    }

    private static void runSlidingWindowCounterDemo() {
        System.out.println("=== Rate Limiter Demo (Sliding Window Counter) ===");
        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        ruleProvider.addRule(new RateLimiterRule("rule-swc", "/external/paid-api",
                REQUESTS_PER_WINDOW, WINDOW_SIZE_MS));

        SlidingWindowCounterStrategy strategy = new SlidingWindowCounterStrategy(storage);
        RateLimiterService service = new RateLimiterService(ruleProvider, strategy,
                new ClientIdKeyResolver());
        RateLimiterController controller = new RateLimiterController(service);

        for (int i = 1; i <= 7; i++) {
            RateLimitRequest request = new RateLimitRequest("Bishwayan",
                    "/external/paid-api", System.currentTimeMillis());
            RateLimitResponse response = controller.checkAccess(request);
            printResponse(i, response);
        }
    }

    private static void runTokenBucketDemo() {
        System.out.println("=== Rate Limiter Demo (Token Bucket) ===");
        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        ruleProvider.addRule(new RateLimiterRule("rule-tb", "/external/paid-api",
                REQUESTS_PER_WINDOW, WINDOW_SIZE_MS));

        TokenBucketStrategy strategy = new TokenBucketStrategy(storage);
        RateLimiterService service = new RateLimiterService(ruleProvider, strategy,
                new ClientIdKeyResolver());
        RateLimiterController controller = new RateLimiterController(service);

        for (int i = 1; i <= 7; i++) {
            RateLimitRequest request = new RateLimitRequest("Bish",
                    "/external/paid-api", System.currentTimeMillis());
            RateLimitResponse response = controller.checkAccess(request);
            printResponse(i, response);
        }
    }

    private static void runExternalResourceFlowDemo() {
        System.out.println("=== External Resource Flow (Question Use Case) ===");
        System.out.println("User Bish is allowed 5 external calls per minute.");
        System.out.println();

        InMemoryStorage storage = new InMemoryStorage();
        InMemoryRuleProvider ruleProvider = new InMemoryRuleProvider();
        ruleProvider.addRule(new RateLimiterRule("rule-ext", "/external/paid-api",
                REQUESTS_PER_WINDOW, WINDOW_SIZE_MS));

        FixedWindowStrategy strategy = new FixedWindowStrategy(storage);
        RateLimiterService limiterService = new RateLimiterService(ruleProvider,
                strategy, new ClientIdKeyResolver());

        ExternalResourceGateway gateway = new ExternalResourceGateway();
        InternalService internalService = new InternalService(limiterService, gateway);

        String[] payloads = {
            "INTERNAL:cache-hit",
            "EXTERNAL:fetch-price",
            "EXTERNAL:fetch-weather",
            "INTERNAL:local-compute",
            "EXTERNAL:translate-text",
            "EXTERNAL:analyze-image",
            "EXTERNAL:detect-fraud",
            "EXTERNAL:send-sms",
            "INTERNAL:log-event"
        };

        for (int i = 0; i < payloads.length; i++) {
            String result = internalService.handleRequest("Bish", payloads[i]);
            System.out.println("Request " + (i + 1) + " [" + payloads[i] + "]: " + result);
        }
    }

    private static void printResponse(int requestNumber, RateLimitResponse response) {
        if (response.isAllowed()) {
            System.out.println("Request " + requestNumber + ": ALLOWED (retryAfter=0ms)");
        } else {
            System.out.println("Request " + requestNumber + ": REJECTED (retryAfter="
                    + response.retryAfterMs() + "ms) \u2014 " + response.message());
        }
    }
}
