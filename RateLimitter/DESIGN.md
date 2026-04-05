# Rate Limiter — Design Document

## What It Does

This is a pluggable rate limiting system for **external resource usage**, not
for incoming client API requests. The key insight: not every client request
triggers an external call. Business logic runs first, and only when an
external resource (a paid third-party API) is needed does the rate limiter get
consulted.

The system supports four algorithms — Fixed Window Counter, Sliding Window
Counter, Token Bucket, and Sliding Window Log — all pluggable through a
single strategy interface. Swapping algorithms requires zero changes to
business logic. The rate limiting key is also pluggable: it can be per
customer, per tenant, per API key, or per external provider.

---

## Architecture

```
  Client Request
       │
       ▼
  ┌────────────────┐
  │ InternalService │──── business logic decides ───┐
  └────────────────┘                                │
       │                                            │
       │ needs external call?                       │ no external call needed
       ▼                                            ▼
  ┌─────────────────────┐                     return result
  │  RateLimiterService │                     (no quota consumed)
  │    (orchestrator)    │
  └──┬───────────┬──────┘
     │           │
     │           │ isAllowed()
     ▼           ▼
┌──────────┐  ┌──────────────────┐
│RuleProvider│  │ RateLimitStrategy │
│(interface)│  │   (interface)     │
└──────────┘  └────────┬─────────┘
     │                 │
     ▼                 ├─► FixedWindowStrategy
┌──────────────┐       ├─► SlidingWindowCounterStrategy
│InMemoryRule  │       ├─► TokenBucketStrategy
│  Provider    │       └─► SlidingWindowStrategy (Log)
└──────────────┘
                       │ read/write state
                       ▼
                ┌──────────────────┐
                │ RateLimitStorage │
                │   (interface)    │
                └────────┬─────────┘
                         │
                         ▼
                ┌──────────────────┐
                │  InMemoryStorage │
                └──────────────────┘
```

---

## Request Flow

```
  InternalService.handleRequest(clientId, payload)
       │
       ▼
  Does business logic need external call?
       │
       ├── NO ──► return internal result (no rate limiting)
       │
       ▼ YES
  Build RateLimitRequest(clientId, "/external/paid-api", now)
       │
       ▼
  RateLimiterService.checkAccess(request)
       │
       ▼
  KeyResolver resolves request ──► composite key
       │
       ▼
  RuleProvider looks up rule for endpoint
       │
       ├── No rule ──► ALLOWED (open policy)
       │
       ▼
  Strategy.isAllowed(key, rule)
       │
       ├── ALLOWED ──► call ExternalResourceGateway
       │
       └── REJECTED ──► return rate-limited message with retryAfterMs
```

---

## Algorithms and Trade-offs

### Fixed Window Counter
Divides time into fixed windows (e.g., one-minute intervals). Each window has
a counter that increments per request and resets when the window expires.

**Pros:** Simple, O(1) time and space per key, very fast.

**Cons:** Susceptible to the boundary burst problem. If a client sends
all their requests at the end of one window and the start of the next, they
effectively get double the limit in a short period.

### Sliding Window Counter
Combines two adjacent fixed windows using a weighted average. The previous
window's count is weighted by how far into the current window the request
occurs (the earlier in the window, the higher the weight of the previous
window's count).

**Pros:** Smooths out the boundary burst problem of fixed windows. Still O(1)
time and space per key. Good balance of accuracy and efficiency.

**Cons:** Approximation — not exact. The weighted average is a statistical
estimate, not a precise count. Under extreme edge cases, a few extra
requests might slip through compared to a perfect sliding window.

### Token Bucket
Starts with a full bucket of tokens. Each request consumes one token. Tokens
refill lazily based on elapsed time since the last refill.

**Pros:** Allows controlled bursts. Good for APIs that need to handle
traffic spikes. No background threads — refill is calculated on-demand.

**Cons:** More complex refill math. Burst capacity can be a problem if the
external provider does not handle bursts well.

### Sliding Window Log
Stores exact timestamps of all requests in a queue. Evicts entries older than
the window, then checks if the count is below the limit.

**Pros:** Most precise — no approximation. Every request is tracked exactly.

**Cons:** O(n) space per key where n is the number of allowed requests in
the window. Eviction scan is O(n) per request. Not suitable for large limits.

---

## Key Resolution

The `KeyResolver` interface decouples how the rate limiting key is built from
the rest of the system. The default `ClientIdKeyResolver` builds keys as
`clientId:endpoint`. But the system supports arbitrary keying strategies:

- **Per customer:** `clientId:endpoint` (default)
- **Per tenant:** `tenantId:endpoint`
- **Per API key:** `apiKey:endpoint`
- **Per external provider:** `providerName:clientId`

Swapping a resolver requires one line change at construction time. No
strategy, service, or storage code changes.

---

## Domain Models

- **RateLimitRequest** — Immutable record: `clientId`, `endpoint`, `timestamp`.
- **RateLimitResponse** — Immutable record: `isAllowed`, `retryAfterMs`, `message`.
- **RateLimiterRule** — Immutable record: `ruleId`, `endpoint`, `maxTokens`, `windowSizeMs`.
  Examples: `100 requests per 60000ms` (100/min), `1000 requests per 3600000ms` (1000/hr).

---

## Concurrency Design

No `synchronized` blocks are used anywhere in the system. Thread safety comes
from `java.util.concurrent` primitives:

- `AtomicInteger` and `AtomicLong` for lock-free counters and timestamps.
- `ConcurrentLinkedQueue` for the sliding window log.
- `ConcurrentHashMap` for storage and rule maps.
- `ConcurrentHashMap.putIfAbsent` for atomic state creation (prevents the
  race where two threads both create a new state for the same key).
- CAS (compare-and-set) spin loops for counter increments and decrements,
  ensuring no thread can push the count past the configured limit.

---

## External Resource Flow (Question Use Case)

The `InternalService` class demonstrates the exact flow from the question:

1. Client calls the API.
2. `InternalService.handleRequest` runs business logic.
3. If no external call is needed, the request is handled internally and no
   rate limit quota is consumed.
4. If an external call is needed, `RateLimiterService.checkAccess` is called.
5. If allowed, `ExternalResourceGateway.callExternalApi` is called.
6. If rate-limited, the request is gracefully rejected with a retry message.

This means: 9 client requests might only consume 5 rate limit slots, because
4 of them were handled internally without touching the external resource.

---

## Key Decisions

- **Rate limit at the external call boundary, not the API boundary.** This
  matches the question requirement: not every API request costs money, only
  external resource calls do.
- **`getOrCreateClientState` using `putIfAbsent`.** Prevents the race where
  concurrent first-access threads each create separate state objects, causing
  the counter to go above the limit.
- **CAS spin loops for counter operations.** `compareAndSet` in a `while(true)`
  loop guarantees that two threads cannot both increment past the limit. If
  the CAS fails, the thread retries with the fresh value.
- **Lazy token refill.** No background threads or timers. Refill is
  calculated on-demand from elapsed time.
- **Constructor injection everywhere.** Every dependency is explicit. No
  singletons, no service locators.

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
| `KeyResolver.java` | interface | Key Resolution Contract |
| `FixedWindowStrategy.java` | class | Strategy Impl |
| `SlidingWindowCounterStrategy.java` | class | Strategy Impl |
| `TokenBucketStrategy.java` | class | Strategy Impl |
| `SlidingWindowStrategy.java` | class | Strategy Impl (Log) |
| `FixedWindowState.java` | class | Concurrency State |
| `SlidingWindowCounterState.java` | class | Concurrency State |
| `TokenBucketState.java` | class | Concurrency State |
| `SlidingWindowState.java` | class | Concurrency State |
| `InMemoryStorage.java` | class | Storage Impl |
| `InMemoryRuleProvider.java` | class | Config Impl |
| `ClientIdKeyResolver.java` | class | Key Resolver Impl |
| `RateLimiterService.java` | class | Orchestrator |
| `RateLimiterController.java` | class | API Entry |
| `RateLimitManagementController.java` | class | API Management |
| `ExternalResourceGateway.java` | class | External Resource |
| `InternalService.java` | class | Business Logic |
| `App.java` | class | Demo Driver |

---

## Build and Run

```bash
cd RateLimitter/src
javac com/example/ratelimiter/*.java
java com.example.ratelimiter.App
```
