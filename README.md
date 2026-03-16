# NW Trails Backend (Phase 3)

Spring Boot 3 backend for Group 06 NW Trails.

## Scope

- OAuth2/JWT-style auth endpoints (`/api/v1/auth/login`, `/api/v1/auth/refresh`)
- Landmark and route APIs for Flutter integration
- Check-in API with business rules:
  - same landmark cannot be checked in twice on same day
  - user must be within 50m of target landmark
- Progress API for awards/profile data
- Admin CRUD endpoints for landmarks/routes
- OpenAPI contract at `openapi/nw-trails-v1.yaml`

## Tech Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security + OAuth2 Resource Server (JWT)
- Springdoc OpenAPI UI
- Maven

## Local Run

```bash
mvn clean test
mvn spring-boot:run
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## Demo Accounts

- `student01 / Passw0rd!` (USER)
- `admin01 / AdminPass!` (USER + ADMIN)

## Token Flow

1. `POST /api/v1/auth/login`
2. Use returned `accessToken` as `Authorization: Bearer <token>`
3. Refresh by calling `POST /api/v1/auth/refresh`

## Project Layout

- `src/main/java/.../api`: controllers and request/response DTOs
- `src/main/java/.../service`: auth and core business logic
- `src/main/java/.../config`: security and JWT config
- `openapi/`: source-of-truth API contract

## Notes

- Current implementation uses in-memory seed data to match Flutter stubs quickly.
- MongoDB dependency is ready; replacing in-memory service with repositories is next.
