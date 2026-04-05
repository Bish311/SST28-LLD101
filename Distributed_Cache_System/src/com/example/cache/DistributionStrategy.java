package com.example.cache;

public interface DistributionStrategy {
    int getNodeIndex(String key, int nodeCount);
}
