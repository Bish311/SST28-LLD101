# Rate Limiter — Design Document

## What It Does

The Rate Limiter is an API throttling system that controls how many requests a
client can make to a specific endpoint within a time window. When a client
exceeds their quota, the system rejects subsequent requests and returns a
response telling them exactly how long to wait before retrying. The design
supports two pluggable algorithms — Token Bucket and Sliding Window Log — and
allows rules to be added or removed at runtime without restarting the service.

Every component communicates through interfaces, not concrete classes. The
service layer knows nothing about which algorithm is running. The algorithm
knows nothing about where state is stored. This separation means any layer
can be replaced independently: swap Token Bucket for Leaky Bucket, swap
in-memory storage for Redis, swap hardcoded rules for a database — each change
touches exactly one class.

---

## Architecture

```
                    ┌───────────────────────┐
                    │   RateLimitRequest     │
                    │  (clientId, endpoint,  │
                    │   timestamp)           │
                    └──────────┬────────────┘
                               │
                               ▼
                    ┌───────────────────────┐
                    │ RateLimiterController  │
                    │   checkAccess(req)     │
                    └──────────┬────────────┘
                               │ delegates
                               ▼
                    ┌───────────────────────┐
                    │  RateLimiterService    │
                    │   (orchestrator)       │
                    └──────┬────────┬───────┘
                           │        │
              getRuleFor() │        │ isAllowed()
                           ▼        ▼
               ┌──────────────┐  ┌──────────────────┐
               │ RuleProvider │  │ RateLimitStrategy │
               │  (interface) │  │   (interface)     │
               └──────┬───────┘  └────────┬──────────┘
                      │                   │
                      ▼                   ├──► TokenBucketStrategy
          ┌────────────────────┐          │       │
          │ InMemoryRuleProvider│          │       │ read/write
          │  ConcurrentHashMap │          │       ▼
          └────────────────────┘          │   ┌──────────────┐
                                          │   │RateLimitStorage│
                                          │   │  (interface)   │
                                          │   └──────┬─────────┘
                                          │          │
                                          │          ▼
                                          │   ┌──────────────────┐
                                          │   │ InMemoryStorage   │
                                          │   │ ConcurrentHashMap │
                                          │   └──────────────────┘
                                          │
                                          └──► SlidingWindowStrategy
                                                    │
                                                    │ read/write
                                                    ▼
                                              (same storage)
```

---

## Request Flow

```
  Client sends request
        │
        ▼
  Controller receives RateLimitRequest
        │
        ▼
  Service looks up rule for endpoint
        │
        ├── No rule found ──► return ALLOWED (open policy)
        │
        ▼
  Service builds composite key: clientId + ":" + endpoint
        │
        ▼
  Service calls strategy.isAllowed(key, rule)
        │
        ├── Strategy checks/updates state in storage
        │
        ├── Tokens available OR window not full
        │       │
        │       ▼
        │   return ALLOWED (retryAfter=0, message="OK")
        │
        └── Tokens exhausted OR window full
                │
                ▼
            return REJECTED (retryAfter=windowSizeMs,
                             message="Rate limit exceeded for /endpoint")
```

---

## Domain Models

- **RateLimitRequest** — Immutable record carrying `clientId`, `endpoint`, and
  `timestamp`. The composite key `clientId:endpoint` ensures rate limiting is
  per-client per-endpoint.

- **RateLimitResponse** — Immutable record with `isAllowed`, `retryAfterMs`,
  and `message`. Rejected responses include the window duration as retry
  guidance and a human-readable explanation.

- **RateLimiterRule** — Immutable record defining `ruleId`, `endpoint`,
  `maxTokens` (burst capacity), and `windowSizeMs` (time window). Rules are
  matched by endpoint path.

---

## Strategy Layer

- **TokenBucketStrategy** — Uses lazy refill: no background threads. On each
  request, it calculates elapsed time since the last refill, computes how many
  tokens to restore (proportional to elapsed/window × maxTokens), caps at
  maxTokens, then decrements if tokens remain. State is held in
  `TokenBucketState` using `AtomicInteger` for the token count and
  `AtomicLong` for the last refill timestamp.

- **SlidingWindowStrategy** — Maintains a log of request timestamps in a
  `ConcurrentLinkedQueue`. On each request, it evicts timestamps older than
  `now - windowSizeMs`, then checks if the remaining count is below
  `maxTokens`. If so, the new timestamp is added and the request is allowed.

Both strategies depend on `RateLimitStorage` (the interface), not
`InMemoryStorage` (the concrete class). This means swapping the storage
backend requires zero changes to either strategy.

---

## Storage Layer

- **InMemoryStorage** — Wraps a `ConcurrentHashMap<String, Object>`. The
  generic `<T>` on `getClientState` and `saveClientState` lets different
  strategies store different state shapes (`TokenBucketState` vs
  `SlidingWindowState`) without a shared base class. The unchecked cast is a
  deliberate tradeoff for an in-memory LLD — in a production system, you would
  type-tag the state or use a serialization layer.

---

## Configuration Layer

- **InMemoryRuleProvider** — Stores rules in a `ConcurrentHashMap` keyed by
  endpoint. Supports `addRule` and `removeRule` for runtime configuration
  changes. The `RateLimitManagementController` exposes these operations as an
  API surface.

- When no rule exists for an endpoint, the service follows an **open policy**:
  the request is allowed unconditionally.

---

## Concurrency Design

- **No `synchronized` blocks anywhere.** All thread safety comes from
  `java.util.concurrent` primitives.

- `AtomicInteger` and `AtomicLong` in `TokenBucketState` enable lock-free
  compare-and-swap operations. Multiple threads hitting the same bucket can
  read and update concurrently without serializing.

- `ConcurrentLinkedQueue` in `SlidingWindowState` provides lock-free `offer`
  and `poll` operations for the timestamp log.

- `ConcurrentHashMap` in both `InMemoryStorage` and `InMemoryRuleProvider`
  provides segment-level locking for high-throughput concurrent access.

---

## Key Decisions

- **Lazy refill over background threads** — The token bucket calculates refill
  at request time based on elapsed milliseconds. This avoids managing thread
  lifecycles, timer tasks, and shutdown coordination. The tradeoff is a
  slightly more complex `isAllowed` method, but the system is simpler,
  deterministic, and easier to test.

- **Constructor injection everywhere** — Every class declares its dependencies
  as constructor parameters. No static state, no singletons, no service
  locators. This makes the system trivially testable: swap any dependency
  with a stub.

- **Composite key `clientId:endpoint`** — A single string key avoids nested
  maps and keeps the storage API flat. The colon delimiter is a convention that
  works as long as neither clientId nor endpoint contains a colon.

- **Optional for rule lookup** — `getRuleFor` returns `Optional<RateLimiterRule>`
  instead of a nullable value, forcing the caller to explicitly handle the
  missing-rule case rather than risking a NullPointerException.

- **Records for all models** — Java 17 records provide `equals`, `hashCode`,
  `toString`, and accessor methods with zero boilerplate. They enforce
  immutability by design.

---

## File Manifest

| File | Type | Layer |
|------|------|-------|
| `RateLimitRequest.java` | record | Domain Model |
| `RateLimitResponse.java` | record | Domain Model |
| `RateLimiterRule.java` | record | Domain Model |
| `RateLimitStrategy.java` | interface | Strategy Contract |
| `RateLimitStorage.java` | interface | Storage Contract |
| `RuleProvider.java` | interface | Config Contract |
| `TokenBucketStrategy.java` | class | Strategy Impl |
| `SlidingWindowStrategy.java` | class | Strategy Impl |
| `TokenBucketState.java` | class | Concurrency State |
| `SlidingWindowState.java` | class | Concurrency State |
| `InMemoryStorage.java` | class | Storage Impl |
| `InMemoryRuleProvider.java` | class | Config Impl |
| `RateLimiterService.java` | class | Orchestrator |
| `RateLimiterController.java` | class | API Entry |
| `RateLimitManagementController.java` | class | API Management |
| `App.java` | class | Demo Driver |

---

## Build and Run

```bash
cd RateLimitter/src
javac com/example/ratelimiter/*.java
java com.example.ratelimiter.App
```
