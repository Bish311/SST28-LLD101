package com.example.cache;

import java.util.ArrayList;
import java.util.List;

public class App {
    private static final int NODE_COUNT = 3;
    private static final int CAPACITY_PER_NODE = 3;

    public static void main(String[] args) {
        InMemoryDatabaseService database = createDatabase();
        DistributedCache moduloCache = buildCache(database, new ModuloDistributionStrategy());

        runModuloDemo(moduloCache);
        System.out.println();
        runEvictionDemo(database);
        System.out.println();
        runLfuEvictionDemo(database);
        System.out.println();
        runConsistentHashingDemo(database);
    }

    private static void runModuloDemo(DistributedCache cache) {
        System.out.println("=== Distributed Cache Demo (Modulo Strategy + LRU) ===");

        putAndPrint(cache, "Bish-data-1", "alpha");
        putAndPrint(cache, "Bish-data-2", "beta");
        putAndPrint(cache, "Bish-data-3", "gamma");
        putAndPrint(cache, "Bish-data-4", "delta");
        putAndPrint(cache, "Bish-data-5", "epsilon");

        System.out.println();
        getAndPrint(cache, "Bish-data-1");
        getAndPrint(cache, "Bish-data-2");

        System.out.println();
        System.out.println("--- Cache Miss -> DB Fetch ---");
        getAndPrint(cache, "Bish-db-1");
        getAndPrint(cache, "Bish-db-1");
    }

    private static void runEvictionDemo(InMemoryDatabaseService database) {
        System.out.println("=== Eviction Demo (LRU, capacity=3 per node) ===");

        List<CacheNode> nodes = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            nodes.add(new CacheNode("EvictNode-" + i, new InMemoryCacheStorage(),
                    new LRUEvictionPolicy(), database, CAPACITY_PER_NODE));
        }
        DistributedCache singleNodeCache = new DistributedCache(nodes, new ModuloDistributionStrategy());

        singleNodeCache.put("key-A", "valA");
        singleNodeCache.put("key-B", "valB");
        singleNodeCache.put("key-C", "valC");
        System.out.println("Inserted key-A, key-B, key-C (node full at capacity 3)");

        singleNodeCache.get("key-A");
        System.out.println("Accessed key-A (now most recently used)");

        singleNodeCache.put("key-D", "valD");
        System.out.println("Inserted key-D -> LRU evicts key-B (least recently used)");

        String valA = singleNodeCache.get("key-A");
        String valB = singleNodeCache.get("key-B");
        String valD = singleNodeCache.get("key-D");
        System.out.println("GET key-A: " + (valA != null ? "HIT " + valA : "MISS"));
        System.out.println("GET key-B: " + (valB != null ? "HIT " + valB : "MISS (evicted)"));
        System.out.println("GET key-D: " + (valD != null ? "HIT " + valD : "MISS"));
    }

    private static void runLfuEvictionDemo(InMemoryDatabaseService database) {
        System.out.println("=== LFU Eviction Demo (capacity=3) ===");

        List<CacheNode> nodes = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            nodes.add(new CacheNode("LFUNode-" + i, new InMemoryCacheStorage(),
                    new LFUEvictionPolicy(), database, CAPACITY_PER_NODE));
        }
        DistributedCache cache = new DistributedCache(nodes, new ModuloDistributionStrategy());

        cache.put("key-X", "valX");
        cache.put("key-Y", "valY");
        cache.put("key-Z", "valZ");
        
        cache.get("key-X");
        cache.get("key-X");
        cache.get("key-Y");

        System.out.println("Inserted X, Y, Z. Accessed X twice, Y once. Z has lowest frequency (1).");

        cache.put("key-W", "valW");
        System.out.println("Inserted W -> LFU evicts Z (lowest frequency).");

        String valX = cache.get("key-X");
        String valY = cache.get("key-Y");
        String valZ = cache.get("key-Z");
        String valW = cache.get("key-W");
        
        System.out.println("GET key-X: " + (valX != null ? "HIT " + valX : "MISS"));
        System.out.println("GET key-Y: " + (valY != null ? "HIT " + valY : "MISS"));
        System.out.println("GET key-Z: " + (valZ != null ? "HIT " + valZ : "MISS (evicted)"));
        System.out.println("GET key-W: " + (valW != null ? "HIT " + valW : "MISS"));
    }

    private static void runConsistentHashingDemo(InMemoryDatabaseService database) {
        System.out.println("=== Consistent Hashing Demo ===");
        ConsistentHashingStrategy consistentStrategy = new ConsistentHashingStrategy(NODE_COUNT);
        DistributedCache chCache = buildCache(database, consistentStrategy);

        putAndPrint(chCache, "Bish-data-1", "alpha");
        putAndPrint(chCache, "Bish-data-2", "beta");
        putAndPrint(chCache, "Bish-data-3", "gamma");

        System.out.println();
        getAndPrint(chCache, "Bish-data-1");
        getAndPrint(chCache, "Bish-data-2");
        getAndPrint(chCache, "Bish-data-3");
    }

    private static DistributedCache buildCache(DatabaseService database,
                                                DistributionStrategy strategy) {
        List<CacheNode> nodes = new ArrayList<>();
        for (int i = 0; i < NODE_COUNT; i++) {
            CacheNode node = new CacheNode("Node-" + i, new InMemoryCacheStorage(),
                    new LRUEvictionPolicy(), database, CAPACITY_PER_NODE);
            nodes.add(node);
        }
        return new DistributedCache(nodes, strategy);
    }

    private static InMemoryDatabaseService createDatabase() {
        InMemoryDatabaseService db = new InMemoryDatabaseService();
        db.seed("Bish-db-1", "db-value-1");
        db.seed("Bish-db-2", "db-value-2");
        db.seed("Bish-db-3", "db-value-3");
        db.seed("Bish-db-4", "db-value-4");
        db.seed("Bish-db-5", "db-value-5");
        return db;
    }

    private static void putAndPrint(DistributedCache cache, String key, String value) {
        int nodeIndex = cache.getNodeIndexForKey(key);
        cache.put(key, value);
        System.out.println("PUT " + key + " = " + value + " -> Node-" + nodeIndex);
    }

    private static void getAndPrint(DistributedCache cache, String key) {
        int nodeIndex = cache.getNodeIndexForKey(key);
        String value = cache.get(key);
        if (value != null) {
            System.out.println("GET " + key + " -> Node-" + nodeIndex + " -> " + value);
        } else {
            System.out.println("GET " + key + " -> Node-" + nodeIndex + " -> NULL (not found)");
        }
    }
}
