package com.example.cache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class LFUEvictionPolicy implements EvictionPolicy {
    private final HashMap<String, Integer> frequencyMap;
    private final TreeMap<Integer, LinkedHashSet<String>> frequencyBuckets;
    private final ReentrantLock lock;

    public LFUEvictionPolicy() {
        this.frequencyMap = new HashMap<>();
        this.frequencyBuckets = new TreeMap<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public void recordAccess(String key) {
        lock.lock();
        try {
            if (frequencyMap.containsKey(key)) {
                int oldFrequency = frequencyMap.get(key);
                int newFrequency = oldFrequency + 1;
                frequencyMap.put(key, newFrequency);
                removeFromBucket(oldFrequency, key);
                addToBucket(newFrequency, key);
                return;
            }
            int initialFrequency = 1;
            frequencyMap.put(key, initialFrequency);
            addToBucket(initialFrequency, key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String evict() {
        lock.lock();
        try {
            if (frequencyBuckets.isEmpty()) {
                return null;
            }
            int lowestFrequency = frequencyBuckets.firstKey();
            LinkedHashSet<String> bucket = frequencyBuckets.get(lowestFrequency);
            String victimKey = bucket.iterator().next();
            removeFromBucket(lowestFrequency, victimKey);
            frequencyMap.remove(victimKey);
            return victimKey;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        lock.lock();
        try {
            if (!frequencyMap.containsKey(key)) {
                return;
            }
            int frequency = frequencyMap.get(key);
            removeFromBucket(frequency, key);
            frequencyMap.remove(key);
        } finally {
            lock.unlock();
        }
    }

    private void addToBucket(int frequency, String key) {
        if (!frequencyBuckets.containsKey(frequency)) {
            frequencyBuckets.put(frequency, new LinkedHashSet<>());
        }
        frequencyBuckets.get(frequency).add(key);
    }

    private void removeFromBucket(int frequency, String key) {
        LinkedHashSet<String> bucket = frequencyBuckets.get(frequency);
        if (bucket == null) {
            return;
        }
        bucket.remove(key);
        if (bucket.isEmpty()) {
            frequencyBuckets.remove(frequency);
        }
    }
}
