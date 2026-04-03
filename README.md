# NW Trails Backend

Spring Boot backend for Group 06 NW Trails.

## Current API Coverage

- Auth: `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/me`, `/api/v1/auth/logout`
- Landmarks: list/detail + admin CRUD
- Routes: list/detail/start + admin CRUD
- Check-ins: create/list + private photo upload/read
- Progress: `/api/v1/progress/me`

## Tech Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security (JWT bearer)
- MongoDB
- Maven

## Prerequisites

1. Java 21 installed
2. Maven installed
3. MongoDB running locally on `localhost:27017`

Default DB URI (from `application.properties`):

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/nw_trails
```

## Local Run

From `proj/backend/nw-trails-backend`:

```bash
mvn clean test
mvn spring-boot:run
```

Server base URL:

- `http://localhost:8080/api/v1`

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## Seeded Test Accounts

- `student01 / Passw0rd!` (`USER`)
- `admin01 / AdminPass!` (`USER`, `ADMIN`)

Users are seeded into MongoDB on first run.

## Quick API Smoke Test (curl)

### 1) Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student01","password":"Passw0rd!"}'
```

Copy `accessToken` and `refreshToken` from response.

### 2) Current user

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

### 3) Landmarks list

```bash
curl http://localhost:8080/api/v1/landmarks \
  -H "Authorization: Bearer <accessToken>"
```

### 4) Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Check-in Photo Storage Notes

- Photo upload endpoint: `POST /api/v1/checkins/photos`
- Photo read endpoint: `GET /api/v1/checkins/photos/{photoId}`
- Storage location: local `storage/checkin-photos/`
- Visibility: private (owner-only)

## Project Layout

- `src/main/java/.../api`: controllers + DTOs
- `src/main/java/.../service`: business/auth logic
- `src/main/java/.../config`: security/JWT/seeders
- `src/main/java/.../repository`: Mongo repositories
- `openapi/`: API contract file

## Notes

- The OpenAPI YAML may lag behind newest endpoints until synced.
- If tests fail with `MongoTimeoutException`, ensure local MongoDB is running.
- If login fails after pulling new auth changes, reseed users:

  ```bash
  mongosh
  use nw_trails
  db.users.drop()
  ```

  Then restart backend (`mvn spring-boot:run`) so seed users are recreated.
