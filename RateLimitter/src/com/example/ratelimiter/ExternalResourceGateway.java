package com.example.ratelimiter;

public class ExternalResourceGateway {

    public String callExternalApi(String clientId, String payload) {
        return "ExternalResult[client=" + clientId + ", data=" + payload + "]";
    }
}
