package com.example.cache;

import java.util.TreeMap;

public class ConsistentHashingStrategy implements DistributionStrategy {
    private static final int VIRTUAL_NODES_PER_NODE = 150;
    private final TreeMap<Integer, Integer> ring;

    public ConsistentHashingStrategy(int nodeCount) {
        this.ring = new TreeMap<>();
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            for (int v = 0; v < VIRTUAL_NODES_PER_NODE; v++) {
                String virtualNodeLabel = "Node-" + nodeIndex + "-VN-" + v;
                int hashValue = hashFunction(virtualNodeLabel);
                ring.put(hashValue, nodeIndex);
            }
        }
    }

    @Override
    public int getNodeIndex(String key, int nodeCount) {
        int keyHash = hashFunction(key);
        Integer ceilingKey = ring.ceilingKey(keyHash);
        if (ceilingKey == null) {
            ceilingKey = ring.firstKey();
        }
        return ring.get(ceilingKey);
    }

    private int hashFunction(String input) {
        int hash = 7;
        for (int i = 0; i < input.length(); i++) {
            hash = hash * 31 + input.charAt(i);
        }
        return Math.abs(hash);
    }
}
