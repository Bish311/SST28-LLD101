package com.example.cache;

public interface EvictionPolicy {
    void recordAccess(String key);
    String evict();
    void remove(String key);
}
