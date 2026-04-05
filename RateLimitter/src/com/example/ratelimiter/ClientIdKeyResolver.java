package com.example.ratelimiter;

public class ClientIdKeyResolver implements KeyResolver {

    @Override
    public String resolve(RateLimitRequest request) {
        return request.clientId() + ":" + request.endpoint();
    }
}
