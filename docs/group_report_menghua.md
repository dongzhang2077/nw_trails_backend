## 4.x API Documentation & Swagger Integration Review

### 4.x.1 OpenAPI Contract Overview

The backend API for Group 06 is specified by a single OpenAPI 3.0.3 contract file `openapi/nw-trails-v1.yaml` located in the backend repository. [file:79]  
This contract defines all required endpoints, request/response schemas, security schemes, and error formats to support the Flutter NW Trails app (landmarks, routes, location-based check-in, awards/progress, and auth). [file:79]  

The contract exposes a single server base URL:

- `http://localhost:8080/api/v1` (local development),

and organizes endpoints under six tags: **Auth**, **Landmarks**, **Routes**, **CheckIns**, **Progress**, and **Admin**. [file:79]  


### 4.x.2 Endpoint Implementation vs OpenAPI

The backend controllers implement the key paths defined in `nw-trails-v1.yaml` with matching HTTP methods and URL patterns. [file:79][file:95][file:96][file:97][file:93][file:94]  

- **Auth**  
  - `AuthController` is mapped to `/api/v1/auth`, exposing `POST /login` and `POST /refresh`, which correspond to `/auth/login` and `/auth/refresh` in the OpenAPI contract when combined with the `/api/v1` server prefix. [file:79][file:95]  
  - Request DTOs `LoginRequest` and `RefreshTokenRequest` match the `LoginRequest` and `RefreshTokenRequest` schemas (fields `username`, `password`, `refreshToken`), and the response DTO `AuthTokenResponse` plus nested `UserSummaryResponse` align with the `AuthTokenResponse` and `UserSummary` schemas (accessToken, refreshToken, tokenType, expiresInSeconds, user). [file:79][file:105][file:106][file:101][file:110]  

- **Landmarks / Admin Landmarks**  
  - `LandmarkController` implements `/landmarks`, `/landmarks/{landmarkId}` and `/admin/landmarks` (POST), `/admin/landmarks/{landmarkId}` (PUT, DELETE), matching the contract’s Landmarks and Admin paths. [file:79][file:96]  
  - DTOs `CreateLandmarkRequest` and `UpdateLandmarkRequest` mirror the `CreateLandmarkRequest` and `UpdateLandmarkRequest` schemas (name, category, address, description, latitude, longitude, imageUrl, rating). [file:79][file:99][file:108]  

- **Routes / Admin Routes**  
  - `RouteController` provides `/routes`, `/routes/{routeId}`, `/routes/{routeId}/start` and admin routes `/admin/routes` (POST) and `/admin/routes/{routeId}` (PUT, DELETE), as specified in the OpenAPI contract. [file:79][file:97]  
  - DTOs `CreateRouteRequest` and `UpdateRouteRequest` correspond to `CreateRouteRequest` / `UpdateRouteRequest` schemas (name, distanceKm, durationMinutes, difficulty, landmarkIds). [file:79][file:104][file:109]  

- **Check-ins & Progress**  
  - `CheckInController` covers `POST /checkins` and `GET /checkins` using `CreateCheckInRequest` and `CheckInResultResponse`/`CheckInRecord` models, consistent with `CreateCheckInRequest`, `CheckInResult`, and `CheckInRecord` schemas in the contract. [file:79][file:93][file:102][file:103]  
  - `ProgressController` implements `GET /progress/me` and returns `UserProgressResponse`, which aggregates `BadgeProgressResponse`, `CategoryProgressResponse`, and `RouteProgressResponse`, matching the `UserProgressResponse`, `BadgeProgress`, `CategoryProgress`, and `RouteProgress` schemas. [file:79][file:94][file:111][file:98][file:100][file:107]  

Overall, all required paths from the contract are implemented by controllers with compatible request/response structures, enabling the Flutter client to call the backend according to the OpenAPI spec. [file:79][file:95][file:96][file:97][file:93][file:94]  


### 4.x.3 Error Handling and Security Alignment

The OpenAPI contract defines a shared `bearerAuth` security scheme and standard error responses (`UnauthorizedError`, `ForbiddenError`, `NotFoundError`, and `ErrorResponse` with `code` and `message` fields). [file:79]  
Business-specific error codes for check-ins, such as `DUPLICATE_CHECKIN` (409) and `OUT_OF_RANGE` (422 with distance details), are documented under `/checkins` and mapped to the `ErrorResponse` schema. [file:79]  

In the current codebase, controllers are structured to support these contracts, and security configuration (JWT authentication and admin authorization) is centralized in the Spring Security configuration (owned by the B1/B2 integration work). [file:78]  
At the time of this report, some integration tests fail on this machine due to `MongoTimeoutException` (local MongoDB not running on `localhost:27017`), but this does not affect the static alignment between controller signatures and the documented contracts; final test green status is coordinated by the integration owner. [file:78][cite:1]  


### 4.x.4 Swagger Annotation Cleanup & Improvements

The project uses a contract-first approach, with `nw-trails-v1.yaml` as the single source of truth for endpoint paths, schemas, and error formats. [file:79]  
Swagger/OpenAPI annotations in controllers are kept minimal and focused on grouping and readability rather than redefining schemas, to avoid conflicts with the YAML contract. [file:79]  

Concrete cleanup and improvements applied or proposed:

- Controllers are tagged according to the contract: `AuthController` → `Auth`, `LandmarkController` → `Landmarks`, `RouteController` → `Routes`, `CheckInController` → `CheckIns`, `ProgressController` → `Progress`, and admin endpoints are grouped under `Admin`. [file:79][file:95][file:96][file:97][file:93][file:94]  
- Selected key endpoints include concise `@Operation(summary="...")` descriptions that mirror the OpenAPI `summary` fields, for example the `/checkins` POST business rules and `/progress/me` progress description, improving Swagger UI readability without diverging from the YAML contract. [file:79][file:93][file:94]  
- Schema-level annotations on DTOs are kept lightweight; fields already matching the OpenAPI schema names and types are not redundantly renamed, and any internal or non-contract endpoints (if present) are candidates to be hidden from Swagger via `@Hidden`. [file:79]  

This approach keeps the generated Swagger UI consistent with the YAML contract while minimizing duplication and maintenance overhead. [file:79][web:7]  


### 4.x.5 Submission Readiness Checklist (Backend & Docs)

The following checklist summarizes backend documentation and contract readiness from the B4 (Swagger/reporting) perspective. [cite:1][file:79][file:95][file:96][file:97][file:93][file:94][file:101][file:102][file:103][file:99][file:108][file:104][file:109][file:111][file:98][file:100][file:110][file:78]

- [ ] **Naming and packaging**  
  - Maven project and root package follow the `ca.douglas.csis4280.nwtrails` convention expected for the NW Trails backend.  

- [x] **OpenAPI contract file present**  
  - `openapi/nw-trails-v1.yaml` exists, with version `1.0.0`, server `http://localhost:8080/api/v1`, and tags `Auth`, `Landmarks`, `Routes`, `CheckIns`, `Progress`, `Admin`. [file:79]  

- [x] **Core endpoints implemented as per contract**  
  - Auth: `/auth/login`, `/auth/refresh` implemented in `AuthController` with matching request/response DTOs. [file:79][file:95][file:105][file:106][file:101][file:110]  
  - Landmarks/Admin: `/landmarks`, `/landmarks/{landmarkId}`, `/admin/landmarks`, `/admin/landmarks/{landmarkId}` implemented in `LandmarkController` using `CreateLandmarkRequest`/`UpdateLandmarkRequest`. [file:79][file:96][file:99][file:108]  
  - Routes/Admin: `/routes`, `/routes/{routeId}`, `/routes/{routeId}/start`, `/admin/routes`, `/admin/routes/{routeId}` implemented in `RouteController` using `CreateRouteRequest`/`UpdateRouteRequest`. [file:79][file:97][file:104][file:109]  
  - Check-ins: `POST /checkins` and `GET /checkins` implemented in `CheckInController` with `CreateCheckInRequest`, `CheckInResultResponse`, and `CheckInRecord` models. [file:79][file:93][file:102][file:103]  
  - Progress: `GET /progress/me` implemented in `ProgressController` returning `UserProgressResponse` with nested badge/category/route progress. [file:79][file:94][file:111][file:98][file:100][file:107]  

- [ ] **Swagger UI and generated docs**  
  - Swagger UI is exposed by Springdoc/OpenAPI for the `/api/v1` endpoints; tags and summaries reflect the contract after minimal annotation cleanup.  

- [ ] **Automated test status (current machine)**  
  - `mvn test` currently fails on this machine because integration tests attempt to connect to MongoDB at `localhost:27017`, resulting in `MongoTimeoutException` due to no running Mongo instance; integration owner will ensure tests pass in a properly configured environment. [file:78]  

This section documents that the backend implementation has been reviewed against the `nw-trails-v1.yaml` contract, with key endpoints, DTOs, and Swagger grouping aligned to support the Flutter client and final submission packaging.
