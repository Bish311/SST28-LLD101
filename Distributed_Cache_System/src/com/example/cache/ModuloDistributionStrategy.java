package com.example.cache;

public class ModuloDistributionStrategy implements DistributionStrategy {

    @Override
    public int getNodeIndex(String key, int nodeCount) {
        return Math.abs(key.hashCode() % nodeCount);
    }
}
