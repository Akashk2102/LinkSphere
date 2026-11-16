# LinkSphere — Scalable URL Shortener & Analytics Platform

LinkSphere is an enterprise-grade URL shortener and click-stream analytics engine built with **Java 21**, **Spring Boot 3**, **PostgreSQL**, **Redis**, and **React + TypeScript**.

## Architecture Highlights
- **Sub-millisecond Redirections**: Cache-Aside pattern utilizing Redis with automatic TTL and cache invalidation.
- **Asynchronous Click Analytics**: Non-blocking click tracking using Spring's `@Async` event pipeline.
- **Stateless Authentication**: Spring Security 6 with JSON Web Tokens (JWT) and BCrypt password hashing.
- **Database Evolution**: Managed Flyway schema migrations with indexed lookups on short codes and composite user filters.
- **Containerization**: Multi-stage Docker builds and Docker Compose for orchestration.

---

## Project Structure
```
linksphere/
├── backend/                  # Spring Boot 3 & Java 21 REST API
│   ├── src/main/java/        # Application code (Controllers, Services, Repositories)
│   ├── src/main/resources/   # Configurations & Flyway migrations
│   └── pom.xml               # Maven dependencies and build definitions
├── frontend/                 # React 18 + TypeScript + Vite Dashboard
├── docker/                   # Docker configurations & container manifests
└── docker-compose.yml        # PostgreSQL & Redis infrastructure
```

---

## Quickstart (Local Development)

### 1. Start Database & Cache
```bash
docker compose up -d
```

### 2. Run Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
