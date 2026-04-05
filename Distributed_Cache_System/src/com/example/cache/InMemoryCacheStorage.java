package com.example.cache;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheStorage implements CacheStorage {
    private final ConcurrentHashMap<String, String> store;

    public InMemoryCacheStorage() {
        this.store = new ConcurrentHashMap<>();
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    @Override
    public String remove(String key) {
        return store.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    @Override
    public int size() {
        return store.size();
    }
}
