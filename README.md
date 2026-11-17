# LinkSphere — Distributed URL Shortener & Click-Stream Analytics Platform

<div align="center">

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![React](https://img.shields.io/badge/React-18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

<p align="center">
  <b>A production-grade, distributed URL shortening engine engineered for sub-millisecond redirection lookups, atomic click tracking, and privacy-first GDPR-compliant analytics.</b>
</p>

[Architecture Deep Dive](docs/ARCHITECTURE.md) • [API Documentation](docs/API_DOCUMENTATION.md) • [Postman Collection](docs/LinkSphere.postman_collection.json)

</div>

---

## 📑 Table of Contents
- [Executive Overview](#-executive-overview)
- [System Architecture & Data Flow](#-system-architecture--data-flow)
- [Key Features](#-key-features)
- [Capacity & Permutation Math](#-capacity--permutation-math)
- [Database Schema & Indexing Strategy](#-database-schema--indexing-strategy)
- [API Reference & Swagger](#-api-reference--swagger)
- [Quickstart: Run with Docker Compose](#-quickstart-run-with-docker-compose)
- [Manual Local Development](#-manual-local-development)
- [Production Kubernetes Deployment](#-production-kubernetes-deployment)

---

## 🚀 Executive Overview

LinkSphere addresses the high read-to-write ratio (~100:1 to 500:1) inherent to URL shorteners. By leveraging the **Cache-Aside Pattern** with Redis, read requests bypass the relational database entirely, achieving sub-millisecond response times. Click analytics ingestion is decoupled from the user-facing request thread via a dedicated **Spring `@Async` TaskExecutor**, ensuring analytics writes never increase redirect latency.

### Tech Stack
| Tier | Technology | Purpose |
| :--- | :--- | :--- |
| **Backend API** | Java 21, Spring Boot 3.3.5 | Core business logic, validation, security, scheduling |
| **Security** | Spring Security 6, JJWT 0.12.6, BCrypt | Stateless JWT auth, role validation, password hashing |
| **Database** | PostgreSQL 16 + Flyway | ACID-compliant persistent storage with managed migrations |
| **Caching & Rate Limiting** | Redis 7 + Spring Data Redis | Cache-aside mapping, atomic sliding-window rate limiters |
| **Frontend** | React 18, TypeScript, Vite, Recharts | Glassmorphic dashboard with real-time analytics graphs |
| **Infra & DevOps** | Docker, Docker Compose, Kubernetes, GitHub Actions | Multi-stage container builds, HPA, CI/CD pipeline |

---

## 🏛 System Architecture & Data Flow

```
                                  Client Browser
                                        │
                             GET /r/{shortCode}
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │        Nginx / API Gateway        │
                      └─────────────────┬─────────────────┘
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │    Spring Boot Application Pod    │
                      │   (Correlation ID & Rate Limit)   │
                      └─────────┬───────────────────┬─────┘
                                │                   │
                       1. Check │                   │ 3. Non-blocking
                          Cache │                   │    Async Event
                                ▼                   ▼
                      ┌───────────────────┐  ┌─────────────────────┐
                      │    Redis Cache    │  │ ThreadPoolExecutor  │
                      │   link:{code}     │  │  (AnalyticsWorker)  │
                      └─────────┬─────────┘  └──────────┬──────────┘
                                │ (Cache Miss)          │
                                ▼                       │ 4. Batch/Write
                      ┌───────────────────┐             │
                      │   PostgreSQL 16   │◄────────────┘
                      │   (short_urls)    │
                      └───────────────────┘
```

### Request Lifecycle Decisions
1. **HTTP 302 Found vs 301 Moved Permanently**:
   - `301` instructs browsers to cache the destination indefinitely. Future visits bypass LinkSphere completely, blinding analytics and preventing link deactivation or expiration.
   - `302 Found` (or `307 Temporary Redirect`) forces client browsers to hit LinkSphere on every click, allowing real-time click counting, expiration validation, and security rate limiting.
2. **Cache Invalidation on Mutation**:
   - Modifying a link's target, toggling its active status, or deleting it immediately triggers `DEL link:{shortCode}` in Redis. Subsequent clicks safely fall through to PostgreSQL without serving stale targets.

---

## ✨ Key Features

- **Base62 Collision Avoidance**: Generates 7-character alphanumeric codes with cryptographically secure random distribution, collision retries, and reserved-word filtering.
- **Custom Vanity Aliases**: Users can define branded short links (e.g. `/r/summer-sale`) protected against duplicate usage and reserved paths.
- **Sub-Millisecond Redirection**: In-memory Redis lookup serves high-velocity links with negligible latency.
- **Privacy-Preserving Analytics (GDPR-Compliant)**:
  - Client IPs are never saved in raw text. They are hashed using SHA-256 with a secure server salt (`IpHasher`).
  - Automatically parses `User-Agent` strings into Device Type (Mobile, Desktop, Tablet), Browser, and OS.
- **Atomic Concurrency Protection**: Clicks are counted via database-level atomic increments (`UPDATE short_urls SET click_count = click_count + 1 WHERE id = :id`), eliminating lost updates.
- **Automated Expiration Cleanup**: An hourly `@Scheduled` background worker deactivates expired links and flushes stale Redis keys.
- **Sliding-Window Rate Limiting**: Redis-backed atomic limiter protects the public redirect endpoint (120 req/min/IP) and creation endpoint (30 req/min/user).
- **Interactive Analytics Dashboard**: Modern React UI with 30-day activity timelines, device breakdowns, browser distributions, and top referrers.

---

## 🧮 Capacity & Permutation Math

LinkSphere generates 7-character Base62 keys from the alphabet `[0-9a-zA-Z]`:

$$\text{Total Combinations} = 62^7 = 3,521,614,606,208 \approx 3.52 \text{ Trillion}$$

At an aggressive ingestion velocity of **1,000 new links generated every second**:

$$\text{Years to Exhaust} = \frac{3.52 \times 10^{12}}{1,000 \times 86,400 \times 365} \approx 111.6 \text{ Years}$$

---

## 🗄 Database Schema & Indexing Strategy

Managed via versioned **Flyway** migration scripts ([V1__init_schema.sql](backend/src/main/resources/db/migration/V1__init_schema.sql)).

```
┌───────────────────────────┐          ┌───────────────────────────┐
│           users           │          │        short_urls         │
├───────────────────────────┤          ├───────────────────────────┤
│ id (PK)                   │1        *│ id (PK)                   │
│ email (UNIQUE)            ├─────────►│ user_id (FK -> users.id)  │
│ password_hash             │          │ original_url (TEXT)       │
│ full_name                 │          │ short_code (VARCHAR, UK)  │
│ role                      │          │ title                     │
│ created_at, updated_at    │          │ is_custom_alias           │
└───────────────────────────┘          │ is_active                 │
                                       │ expires_at (INDEX)        │
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
                                       │ device_type, browser, os  │
                                       │ country_code              │
                                       │ ip_hash (SHA-256 salted)  │
                                       └───────────────────────────┘
```

### Optimized Indexes
- **`uk_short_urls_code`**: Unique B-Tree index on `short_code` enables $O(\log N)$ point lookups on cache misses.
- **`idx_short_urls_user_created`**: Composite index on `(user_id, created_at DESC)` ensures instant dashboard pagination without full-table sorting.
- **`idx_short_urls_expires_at`**: Partial index `WHERE expires_at IS NOT NULL` optimizes background cleanup queries while saving index memory.
- **`idx_analytics_url_timestamp`**: Composite index on `(short_url_id, clicked_at DESC)` powers rapid 30-day analytics aggregation.

---

## 📡 API Reference & Swagger

Interactive Swagger UI is accessible at:
```http
http://localhost:8080/swagger-ui.html
```

| Method | Endpoint | Access | Description |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new user account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & receive JWT Bearer token |
| `GET` | `/api/v1/auth/me` | User | Retrieve current user profile |
| `POST` | `/api/v1/urls` | User | Create shortened link with optional alias & expiry |
| `GET` | `/api/v1/urls` | User | List user's links (supports search & pagination) |
| `GET` | `/api/v1/urls/{id}` | User | Get link details |
| `PUT` | `/api/v1/urls/{id}` | User | Update title, expiration date, or active status |
| `DELETE`| `/api/v1/urls/{id}` | User | Delete link and evict from Redis cache |
| `GET` | `/api/v1/urls/{id}/analytics` | User | Fetch click metrics, timeline, and device stats |
| `GET` | `/r/{shortCode}` | Public | **Public redirect** returning HTTP 302 Found |
| `GET` | `/actuator/health` | Public | Application health & readiness check |

---

## ⚡ Quickstart: Run with Docker Compose

The fastest way to spin up the entire cluster (PostgreSQL, Redis, Spring Boot backend, and React frontend):

```bash
# Clone the repository
git clone https://github.com/Akashk2102/LinkSphere.git
cd LinkSphere

# Start all 4 services
docker compose up --build
```

### Access Points
- **React Frontend**: [http://localhost:3000](http://localhost:3000)
- **Spring Boot API**: [http://localhost:8080](http://localhost:8080)
- **Swagger Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Pre-Seeded Demo Account**:
  - **Email**: `demo@linksphere.dev`
  - **Password**: `Demo12345!`
  *(Includes pre-seeded links with 250+ historical clicks and charts ready to inspect)*

---

## 💻 Manual Local Development

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL & Redis (running locally or via Docker)

### 1. Database & Cache
```bash
docker compose up postgres redis -d
```

### 2. Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173).

---

## 🚢 Production Kubernetes Deployment

Kubernetes manifests are structured under the [k8s/](k8s/) directory:

```bash
# Deploy persistent storage, PostgreSQL, and Redis
kubectl apply -f k8s/datastore.yaml

# Deploy backend with HPA (2-10 pods), frontend, and ingress
kubectl apply -f k8s/apps.yaml
```

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
