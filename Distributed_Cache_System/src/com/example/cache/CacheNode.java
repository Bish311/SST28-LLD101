package com.example.cache;

import java.util.concurrent.locks.ReentrantLock;

public class CacheNode {
    private final CacheStorage storage;
    private final EvictionPolicy evictionPolicy;
    private final DatabaseService databaseService;
    private final int maxCapacity;
    private final String nodeId;
    private final ReentrantLock lock;

    public CacheNode(String nodeId, CacheStorage storage, EvictionPolicy evictionPolicy,
                     DatabaseService databaseService, int maxCapacity) {
        this.nodeId = nodeId;
        this.storage = storage;
        this.evictionPolicy = evictionPolicy;
        this.databaseService = databaseService;
        this.maxCapacity = maxCapacity;
        this.lock = new ReentrantLock();
    }

    public String get(String key) {
        lock.lock();
        try {
            if (storage.containsKey(key)) {
                evictionPolicy.recordAccess(key);
                return storage.get(key);
            }

            String fetchedValue = databaseService.fetch(key);
            if (fetchedValue == null) {
                return null;
            }

            insertEntry(key, fetchedValue);
            return fetchedValue;
        } finally {
            lock.unlock();
        }
    }

    public void put(String key, String value) {
        lock.lock();
        try {
            if (storage.containsKey(key)) {
                storage.put(key, value);
                evictionPolicy.recordAccess(key);
                return;
            }

            insertEntry(key, value);
        } finally {
            lock.unlock();
        }
    }

    private void insertEntry(String key, String value) {
        if (storage.size() >= maxCapacity) {
            String victimKey = evictionPolicy.evict();
            if (victimKey != null) {
                storage.remove(victimKey);
            }
        }
        storage.put(key, value);
        evictionPolicy.recordAccess(key);
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getCurrentSize() {
        return storage.size();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
