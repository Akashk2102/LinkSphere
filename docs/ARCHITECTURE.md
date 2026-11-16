# LinkSphere Architecture & System Design Specification

## 1. System Overview

LinkSphere is a high-throughput, fault-tolerant URL shortener and click-stream analytics engine. It is optimized for high read-to-write ratio traffic (~100:1), sub-millisecond redirection lookups, and privacy-preserving GDPR-compliant analytics tracking.

---

## 2. High-Level Architecture Diagram

```
                              ┌────────────────────────────────────────┐
                              │            Clients / Browsers          │
                              └───────────────────┬────────────────────┘
                                                  │
                                       GET /r/{shortCode}
                                                  ▼
                         ┌──────────────────────────────────────────────────┐
                         │              Nginx Ingress / CDN                 │
                         └────────────────────────┬─────────────────────────┘
                                                  │
                                                  ▼
                         ┌──────────────────────────────────────────────────┐
                         │           Spring Boot Application Cluster        │
                         │          (Stateless Service Instances)           │
                         └───────────┬───────────────────────────────┬──────┘
                                     │                               │
                            1. Check Cache                  3. Fire Async Event
                                     │                               │
                                     ▼                               ▼
                         ┌───────────────────────┐       ┌───────────────────────┐
                         │      Redis 7.x        │       │  ThreadPoolExecutor   │
                         │ (In-Memory Key-Value) │       │   (AnalyticsWorker)   │
                         └───────────┬───────────┘       └───────────┬───────────┘
                                     │ (Miss)                        │
                                     ▼                               │ 4. Batch/Write
                         ┌───────────────────────┐                   │
                         │     PostgreSQL 16     │◄──────────────────┘
                         │ (Relational & ACID)   │
                         └───────────────────────┘
```

---

## 3. Database Schema (PostgreSQL)

### Entity-Relationship Diagram

```
┌───────────────────────────┐          ┌───────────────────────────┐
│           users           │          │        short_urls         │
├───────────────────────────┤          ├───────────────────────────┤
│ id (PK)                   │1        *│ id (PK)                   │
│ email (UNIQUE)            ├─────────►│ user_id (FK -> users.id)  │
│ password_hash             │          │ original_url (TEXT)       │
│ full_name                 │          │ short_code (VARCHAR, UK)  │
│ role                      │          │ title                     │
│ created_at                │          │ is_custom_alias           │
│ updated_at                │          │ is_active                 │
└───────────────────────────┘          │ expires_at (INDEX)        │
                                       │ click_count               │
                                       │ created_at, updated_at    │
                                       └─────────────┬─────────────┘
                                                     │ 1
                                                     │
                                                     │ *
                                       ┌─────────────▼─────────────┐
                                       │      click_analytics      │
                                       ├───────────────────────────┤
                                       │ id (PK)                   │
                                       │ short_url_id (FK)         │
                                       │ clicked_at (INDEX)        │
                                       │ referrer                  │
                                       │ device_type               │
                                       │ browser                   │
                                       │ operating_system          │
                                       │ country_code              │
                                       │ ip_hash (SHA-256 salted)  │
                                       └───────────────────────────┘
```

### Indexing Strategy
1. **`uk_short_urls_code`**: Unique B-tree index on `short_code`. Provides $O(\log N)$ or immediate B-tree index scan on database fallback.
2. **`idx_short_urls_user_created`**: Composite index on `(user_id, created_at DESC)` ensures instantaneous dashboard pagination without full-table sorting.
3. **`idx_short_urls_expires_at`**: Filtered index `WHERE expires_at IS NOT NULL` optimizes background cleanup tasks.
4. **`idx_analytics_url_timestamp`**: Composite index on `(short_url_id, clicked_at DESC)` powers date-range and 30-day analytics aggregation.

---

## 4. Short-Code Generation Engine

### Math & Capacity Calculation
LinkSphere generates 7-character Base62 alphanumeric codes:
- Characters: `[0-9a-zA-Z]` (62 symbols)
- Total possible permutations:
  $$62^7 = 3,521,614,606,208 \approx 3.52 \text{ Trillion Unique URLs}$$
- Even at a throughput of 1,000 URLs shortened every second:
  $$\frac{3.52 \times 10^{12}}{1000 \times 86400 \times 365} \approx 111 \text{ years of continuous generation}$$

### Collision Strategy
1. Candidate string generated via `SecureRandom` across the 62-character alphabet.
2. Filtered against reserved path keywords (`api`, `login`, `admin`, etc.).
3. Database `existsByShortCode` check with up to 5 retries.
4. Fallback: Automatically expands code length to 8 characters if retry threshold is met.

---

## 5. Cache-Aside Pattern & Invalidation

1. **Read Path (Redirection)**:
   - Query Redis: `GET link:{shortCode}`.
   - On **Hit**: Validate `isActive` and `!isExpired()`. If valid, redirect immediately.
   - On **Miss**: Query PostgreSQL. If exists, populate Redis (`SETEX link:{shortCode} 86400 {json}`).
   - If missing from DB: Return HTTP 404.
2. **Write / Mutation Path**:
   - When a link is updated (e.g. deactivated, expiration modified) or deleted:
     - Mutate PostgreSQL within `@Transactional` boundary.
     - Execute explicit cache eviction: `redisTemplate.delete("link:" + shortCode)`.
     - This guarantees that disabled links stop redirecting immediately.

---

## 6. Privacy & GDPR Compliance

- LinkSphere **never** stores plaintext IPv4 or IPv6 addresses.
- Client IPs are hashed using SHA-256 combined with a secure server-side salt (`IpHasher.hashIp`).
- This allows tracking distinct visits and enforcing anti-scraping rate limits without retaining personally identifiable information (PII).
