package com.example.cache;

import java.util.ArrayList;
import java.util.List;

public class DistributedCache {
    private final List<CacheNode> nodes;
    private final DistributionStrategy distributionStrategy;

    public DistributedCache(List<CacheNode> nodes, DistributionStrategy distributionStrategy) {
        this.nodes = new ArrayList<>(nodes);
        this.distributionStrategy = distributionStrategy;
    }

    public String get(String key) {
        int nodeIndex = distributionStrategy.getNodeIndex(key, nodes.size());
        return nodes.get(nodeIndex).get(key);
    }

    public void put(String key, String value) {
        int nodeIndex = distributionStrategy.getNodeIndex(key, nodes.size());
        nodes.get(nodeIndex).put(key, value);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public CacheNode getNode(int index) {
        return nodes.get(index);
    }

    public int getNodeIndexForKey(String key) {
        return distributionStrategy.getNodeIndex(key, nodes.size());
    }
}
