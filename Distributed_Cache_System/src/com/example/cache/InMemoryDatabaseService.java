package com.example.cache;

import java.util.HashMap;

public class InMemoryDatabaseService implements DatabaseService {
    private final HashMap<String, String> dataStore;

    public InMemoryDatabaseService() {
        this.dataStore = new HashMap<>();
    }

    public void seed(String key, String value) {
        dataStore.put(key, value);
    }

    @Override
    public String fetch(String key) {
        return dataStore.get(key);
    }
}
