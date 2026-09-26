# Redis Caching Architecture & Strategy

## 1. Role in the Hospitality Platform
In hotel booking platforms, read operations (browsing hotels, searching by city, inspecting amenities) outnumber write operations (creating or updating hotel records) by roughly 9 to 1.
Redis serves as an in-memory key-value cache positioned between `hotel-service` and PostgreSQL `hms_hotel_db`.

## 2. Configuration & Eviction
- **Memory Ceiling**: `maxmemory 256mb`
- **Eviction Policy**: `volatile-lru`
  - When memory reaches the 256MB limit, Redis selectively evicts keys that have an explicit TTL expiration set, targeting the Least Recently Used (LRU) keys first.
  - Keys without an expiration set are preserved, preventing accidental data loss of persistent configurations.
- **Persistence**: Hybrid (RDB snapshots every 15 minutes + AOF append-only log with `everysec` fsync) ensures rapid warm restart after container reboots.

## 3. Graceful Degradation (Resilience Pattern)
- If the Redis container experiences network latency or fails completely:
  1. Connection timeout in Spring Boot is set to 500ms.
  2. Spring Cache's `CacheErrorHandler` catches connection exceptions.
  3. Instead of returning HTTP 500 to the customer, the application logs a warning and falls back to querying PostgreSQL directly.
  4. Once Redis recovers, caching resumes automatically.
