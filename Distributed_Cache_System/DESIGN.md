# Distributed Cache — Design Document

## What It Does

This is a distributed in-memory cache that spreads key-value data across
multiple cache nodes. Each node has limited capacity and uses a pluggable
eviction policy to decide what to remove when full. The system sits in front
of a backing database: on a cache hit, it returns instantly; on a cache miss,
it fetches from the database, stores the result in the cache, and returns it.

The two core extensibility points are:
- **Distribution strategy** — decides which node a key lives on.
- **Eviction policy** — decides which entry to remove when a node is full.

Both are pluggable through interfaces. Swapping either requires zero changes
to the rest of the system.

---

## Architecture

```
  Client
    │
    ▼
┌──────────────────────────────────────┐
│        DistributedCache              │
│        (facade — get/put)            │
│                                      │
│   DistributionStrategy decides       │
│   which CacheNode to route to        │
└────────┬────────┬────────┬───────────┘
         │        │        │
         ▼        ▼        ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │ Node-0  │ │ Node-1  │ │ Node-2  │
    │         │ │         │ │         │
    │ Storage │ │ Storage │ │ Storage │
    │ Eviction│ │ Eviction│ │ Eviction│
    │ DB ref  │ │ DB ref  │ │ DB ref  │
    └─────────┘ └─────────┘ └─────────┘
         │        │        │
         └────────┼────────┘
                  ▼
         ┌────────────────┐
         │ DatabaseService │
         │   (fallback)    │
         └────────────────┘
```

---

## Request Flows

### get(key) — Cache Hit
```
  get(key)
    │
    ▼
  DistributionStrategy.getNodeIndex(key, nodeCount)
    │
    ▼
  CacheNode[index].get(key)
    │
    ▼
  storage.containsKey(key) == true
    │
    ▼
  evictionPolicy.recordAccess(key)   ← mark as recently used
    │
    ▼
  return storage.get(key)
```

### get(key) — Cache Miss
```
  get(key)
    │
    ▼
  CacheNode[index].get(key)
    │
    ▼
  storage.containsKey(key) == false
    │
    ▼
  databaseService.fetch(key)
    │
    ├── null → return null (key doesn't exist anywhere)
    │
    ▼ non-null
  insertEntry(key, fetchedValue)     ← may evict if at capacity
    │
    ▼
  return fetchedValue
```

### put(key, value) — With Eviction
```
  put(key, value)
    │
    ▼
  CacheNode[index].put(key, value)
    │
    ▼
  storage.containsKey(key)?
    │
    ├── YES → overwrite value, recordAccess(key), return
    │
    ▼ NO (new key)
  storage.size() >= maxCapacity?
    │
    ├── YES → evictionPolicy.evict() → victimKey
    │         storage.remove(victimKey)
    │
    ▼
  storage.put(key, value)
  evictionPolicy.recordAccess(key)
```

---

## Distribution Strategies

### Modulo (hash(key) % nodeCount)

Simple, O(1) computation. Uses the key's hashCode modulo the number of
nodes. Deterministic — same key always maps to the same node.

The downside: if a node is added or removed, every key potentially remaps.
This is fine for a fixed cluster but not for dynamic scaling.

### Consistent Hashing

Maps each real node to 150 virtual nodes on a hash ring (TreeMap). For any
key, the system computes its hash and walks clockwise to the first virtual
node on the ring. This maps the key to the real node that owns that virtual
node.

When a node is added or removed, only approximately 1/N of the keys need to
be remapped, where N is the number of nodes. The ring is built at
construction time and never mutated, making it inherently thread-safe for
reads.

---

## Eviction Policies

### LRU (Least Recently Used)

Evicts the entry that has not been accessed for the longest time.

Internally uses a doubly-linked list plus a HashMap for O(1) operations:
- `recordAccess(key)` — moves the node to the tail (most recent).
- `evict()` — removes and returns the head node (least recent).
- `remove(key)` — detaches the node from the list.

The list is protected by a ReentrantLock for thread safety.

### LFU (Least Frequently Used)

Evicts the entry with the fewest total accesses. Ties are broken by
insertion order (the oldest among equally-frequent entries is evicted first).

Internally uses:
- `HashMap<String, Integer>` — maps each key to its access frequency.
- `TreeMap<Integer, LinkedHashSet<String>>` — groups keys by frequency.
  The TreeMap's natural ordering gives O(log F) access to the lowest
  frequency bucket. The LinkedHashSet preserves insertion order for
  tie-breaking.

Also protected by a ReentrantLock.

---

## Thread Safety

The system is designed for concurrent access from multiple threads.

- **CacheNode** uses a `ReentrantLock` that protects the entire get/put
  operation as a single atomic unit. This prevents the TOCTOU race between
  containsKey, evict, and put.
- **InMemoryCacheStorage** uses `ConcurrentHashMap` for thread-safe
  key-value storage.
- **LRUEvictionPolicy** uses a `ReentrantLock` to protect doubly-linked
  list mutations.
- **LFUEvictionPolicy** uses a `ReentrantLock` to protect frequency map
  and bucket mutations.
- **ConsistentHashingStrategy** ring is immutable after construction —
  read-only access is inherently safe.
- **ModuloDistributionStrategy** is stateless — pure function.

---

## Domain Models

- **CacheEntry** — Record: `key`, `value`. Immutable data carrier.
- **CacheConfig** — Record: `nodeCount`, `maxCapacityPerNode`. System
  configuration.

---

## Key Design Decisions

- **Facade pattern for DistributedCache.** Clients call `get(key)` and
  `put(key, value)` on a single object. They never see nodes, strategies,
  or eviction policies. The facade picks the right node and delegates.

- **DB fetch inside CacheNode, not the facade.** The cache-miss-to-DB-fetch
  path is the node's responsibility because the node already holds the lock.
  Doing it in the facade would require re-acquiring the lock and
  re-checking containsKey, introducing a race.

- **insertEntry helper.** The eviction-then-store logic is shared between
  put and the DB-fetch path inside get. A private method prevents
  duplication.

- **ReentrantLock over synchronized.** Explicit lock objects allow for
  future upgrades (tryLock, lock timeouts, read-write locks) without
  changing the API. They also make the locking scope clearer in code review.

- **Constructor injection everywhere.** No static state, no singletons.
  Every dependency is explicit.

---

## File Manifest

| # | File | Type | Layer |
|---|------|------|-------|
| 1 | `CacheEntry.java` | record | Domain Model |
| 2 | `CacheConfig.java` | record | Domain Model |
| 3 | `DistributionStrategy.java` | interface | Distribution Contract |
| 4 | `EvictionPolicy.java` | interface | Eviction Contract |
| 5 | `CacheStorage.java` | interface | Storage Contract |
| 6 | `DatabaseService.java` | interface | Database Contract |
| 7 | `ModuloDistributionStrategy.java` | class | Distribution Impl |
| 8 | `ConsistentHashingStrategy.java` | class | Distribution Impl |
| 9 | `LRUEvictionPolicy.java` | class | Eviction Impl |
| 10 | `LFUEvictionPolicy.java` | class | Eviction Impl |
| 11 | `InMemoryCacheStorage.java` | class | Storage Impl |
| 12 | `InMemoryDatabaseService.java` | class | Database Impl |
| 13 | `CacheNode.java` | class | Node Logic |
| 14 | `DistributedCache.java` | class | Facade |
| 15 | `App.java` | class | Demo Driver |

---

## Build and Run

```bash
cd Distributed_Cache_System/src
javac com/example/cache/*.java
java com.example.cache.App
```
