# LinkSphere: System Design & Java Architecture Interview Guide

This guide compiles high-frequency interview questions centered around URL shorteners, distributed caching, database indexing, and asynchronous event streams.

---

## 1. System Design Questions

### Q1: Why did you choose HTTP 302 Found instead of HTTP 301 Moved Permanently for LinkSphere?
**Model Answer**:
- A `301 Moved Permanently` tells the client browser to cache the target URL in its local disk/memory cache. Any subsequent click from that user bypasses our server entirely and jumps straight to the destination.
- In a modern URL shortener, this breaks core business functionality:
  1. We cannot record clicks, referrer information, device types, or timestamps for subsequent visits.
  2. If the creator edits, expires, or deactivates the link, users whose browsers cached the 301 will still access the old destination.
- With `302 Found` (or `307 Temporary Redirect`), the browser makes a roundtrip to LinkSphere every single time, letting us validate link status, enforce rate limits, and record analytics before redirecting.

---

### Q2: How do you handle cache invalidation when a link is updated or disabled?
**Model Answer**:
- LinkSphere uses the **Cache-Aside Pattern** with explicit eviction on write.
- When an update (`PUT /api/v1/urls/{id}`) or deletion occurs:
  1. The changes are committed to PostgreSQL within a database transaction.
  2. The Redis key (`link:{shortCode}`) is evicted (`DEL link:{shortCode}`).
  3. If the link remains active, we can optionally pre-warm the cache immediately with the new payload. If disabled, evicting ensures the next read falls through to the DB, detects `isActive == false`, and immediately returns `410 Gone` without stale redirects.

---

### Q3: How does LinkSphere protect against the Thundering Herd (Cache Stampede) problem?
**Model Answer**:
- When a viral link with millions of hits expires from Redis simultaneously, hundreds of concurrent requests might simultaneously experience a cache miss and query PostgreSQL.
- LinkSphere mitigates this by:
  1. Setting high TTLs (24h) with active refreshes.
  2. In high-scale deployments, implementing a Mutex Lock (via Redis `SETNX` or Redisson) or Probabilistic Early Expiration (XFetch algorithm) ensures only **one** worker thread fetches the URL from PostgreSQL while others wait for the cache to be repopulated.

---

### Q4: How do you scale click tracking without degrading redirect response latency?
**Model Answer**:
- Tracking user clicks involves parsing user agents, hashing IPs, and inserting a row into `click_analytics`. If done synchronously, database latency would degrade redirect response time from 2ms to 30-50ms.
- LinkSphere decouples click tracking using Spring's **asynchronous execution pipeline**:
  1. The `RedirectController` triggers `AnalyticsService.recordClickAsync()` which offloads the payload to a bounded `ThreadPoolTaskExecutor`.
  2. The HTTP 302 redirect response is returned to the user immediately.
  3. For ultra-high traffic (100k+ clicks/sec), we replace `@Async` with an event streaming backbone like **Apache Kafka** or **RabbitMQ**, consuming click events with background workers doing batch inserts (`pg_copy` or bulk insert) into PostgreSQL or a time-series/columnar database like ClickHouse.

---

## 2. Java & Spring Boot Architecture Questions

### Q5: Why is `open-in-view: false` configured in `application.yml`?
**Model Answer**:
- By default, Spring Boot sets `spring.jpa.open-in-view=true`. This maintains the Hibernate persistence context and holds onto database connections throughout the entire HTTP request lifecycle, including during response rendering/serialization.
- Under heavy load, if client networks are slow, database connections in the HikariCP pool get starved, causing request timeouts. Setting `open-in-view: false` confines DB connection borrowing strictly to the `@Transactional` boundary in the Service layer.

---

### Q6: How do you ensure click counts increment atomically under concurrent load?
**Model Answer**:
- Simply doing `shortUrl.setClickCount(shortUrl.getClickCount() + 1)` and `repository.save()` causes lost updates due to race conditions.
- LinkSphere uses an atomic database increment query:
  ```sql
  UPDATE short_urls SET click_count = click_count + 1 WHERE id = :id;
  ```
- This executes an atomic in-place update at the database engine level with row-level locking, ensuring zero lost increments even under high concurrency.
- In Redis, atomic counters can also be maintained using `INCR link:clicks:{shortCode}` and periodically flushed to PostgreSQL in batches.
