package com.example.ratelimiter;

public class InternalService {
    private final RateLimiterService rateLimiterService;
    private final ExternalResourceGateway externalGateway;
    private static final String EXTERNAL_RESOURCE_ENDPOINT = "/external/paid-api";

    public InternalService(RateLimiterService rateLimiterService,
                           ExternalResourceGateway externalGateway) {
        this.rateLimiterService = rateLimiterService;
        this.externalGateway = externalGateway;
    }

    public String handleRequest(String clientId, String payload) {
        boolean needsExternalCall = determineIfExternalCallNeeded(payload);

        if (!needsExternalCall) {
            return "Handled internally for " + clientId + " — no external call needed";
        }

        RateLimitRequest rateLimitCheck = new RateLimitRequest(
                clientId, EXTERNAL_RESOURCE_ENDPOINT, System.currentTimeMillis());
        RateLimitResponse rateLimitResult = rateLimiterService.checkAccess(rateLimitCheck);

        if (!rateLimitResult.isAllowed()) {
            return "RATE_LIMITED: " + rateLimitResult.message()
                    + " (retryAfter=" + rateLimitResult.retryAfterMs() + "ms)";
        }

        return externalGateway.callExternalApi(clientId, payload);
    }

    private boolean determineIfExternalCallNeeded(String payload) {
        return payload != null && payload.startsWith("EXTERNAL:");
    }
}
