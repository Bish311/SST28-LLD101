package com.example.cache;

public interface CacheStorage {
    String get(String key);
    void put(String key, String value);
    String remove(String key);
    boolean containsKey(String key);
    int size();
}
