# LinkSphere REST API Documentation

Base URL: `http://localhost:8080` (or `http://localhost:3000` via Nginx frontend reverse proxy)

All protected endpoints require the HTTP Header:
```http
Authorization: Bearer <your_jwt_token>
```

---

## 1. Authentication Endpoints

### 1.1 Register User
- **Method**: `POST`
- **Path**: `/api/v1/auth/register`
- **Request Body**:
  ```json
  {
    "email": "alex.rivera@example.com",
    "password": "SecurePassword123!",
    "fullName": "Alex Rivera"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "email": "alex.rivera@example.com",
      "fullName": "Alex Rivera",
      "role": "ROLE_USER"
    }
  }
  ```

### 1.2 Login User
- **Method**: `POST`
- **Path**: `/api/v1/auth/login`
- **Request Body**:
  ```json
  {
    "email": "alex.rivera@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response** (`200 OK`): Same structure as Register response.

### 1.3 Get Current Profile
- **Method**: `GET`
- **Path**: `/api/v1/auth/me`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (`200 OK`):
  ```json
  {
    "id": 1,
    "email": "alex.rivera@example.com",
    "fullName": "Alex Rivera",
    "role": "ROLE_USER"
  }
  ```

---

## 2. URL Management Endpoints

### 2.1 Create Short URL
- **Method**: `POST`
- **Path**: `/api/v1/urls`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "originalUrl": "https://spring.io/projects/spring-boot",
    "title": "Spring Boot Documentation",
    "customAlias": "spring-docs",
    "expiresAt": "2027-12-31T23:59:59Z"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 10,
    "shortCode": "spring-docs",
    "shortUrl": "http://localhost:8080/r/spring-docs",
    "originalUrl": "https://spring.io/projects/spring-boot",
    "title": "Spring Boot Documentation",
    "isCustomAlias": true,
    "isActive": true,
    "isExpired": false,
    "clickCount": 0,
    "expiresAt": "2027-12-31T23:59:59Z",
    "createdAt": "2026-11-16T16:20:00Z"
  }
  ```

### 2.2 List User URLs (Paginated & Searchable)
- **Method**: `GET`
- **Path**: `/api/v1/urls?search=spring&page=0&size=10`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (`200 OK`):
  ```json
  {
    "content": [
      {
        "id": 10,
        "shortCode": "spring-docs",
        "shortUrl": "http://localhost:8080/r/spring-docs",
        "originalUrl": "https://spring.io/projects/spring-boot",
        "title": "Spring Boot Documentation",
        "isCustomAlias": true,
        "isActive": true,
        "isExpired": false,
        "clickCount": 142,
        "createdAt": "2026-11-16T16:20:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
  ```

### 2.3 Update URL
- **Method**: `PUT`
- **Path**: `/api/v1/urls/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "title": "Updated Title",
    "isActive": false,
    "expiresAt": "2028-01-01T00:00:00Z"
  }
  ```
- **Response** (`200 OK`): Updated `UrlResponse` object.

### 2.4 Delete URL
- **Method**: `DELETE`
- **Path**: `/api/v1/urls/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (`204 No Content`)

---

## 3. Public Redirection Endpoint

### 3.1 Redirect to Original Destination
- **Method**: `GET`
- **Path**: `/r/{shortCode}`
- **Rate Limit**: 120 requests/minute per client IP
- **Response**:
  - `302 Found`: Redirects to target location via `Location` header.
  - `410 Gone`: If link is disabled or past expiration date.
  - `404 Not Found`: If short code does not exist.
  - `429 Too Many Requests`: If client exceeds rate limits.

---

## 4. Analytics Endpoint

### 4.1 Get URL Click Analytics
- **Method**: `GET`
- **Path**: `/api/v1/urls/{id}/analytics`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (`200 OK`):
  ```json
  {
    "shortUrlId": 10,
    "shortCode": "spring-docs",
    "originalUrl": "https://spring.io/projects/spring-boot",
    "totalClicks": 1420,
    "devices": [
      { "label": "Mobile", "count": 850, "percentage": 59.9 },
      { "label": "Desktop", "count": 520, "percentage": 36.6 },
      { "label": "Tablet", "count": 50, "percentage": 3.5 }
    ],
    "browsers": [
      { "label": "Chrome", "count": 900, "percentage": 63.4 },
      { "label": "Safari", "count": 350, "percentage": 24.6 },
      { "label": "Firefox", "count": 170, "percentage": 12.0 }
    ],
    "referrers": [
      { "label": "Direct", "count": 720, "percentage": 50.7 },
      { "label": "https://twitter.com/", "count": 480, "percentage": 33.8 },
      { "label": "https://linkedin.com/", "count": 220, "percentage": 15.5 }
    ],
    "timeline": [
      { "date": "2026-10-18", "clicks": 32 },
      { "date": "2026-10-19", "clicks": 45 }
    ]
  }
  ```
