# Team Execution Plan (Backend)

## Branching

- Main branch: `main` (protected, PR only)
- Feature branches:
  - `feat/auth-oauth2`
  - `feat/landmarks-routes-crud`
  - `feat/checkin-progress`
  - `feat/swagger-reporting`

## Difficulty-Ranked Assignment (High -> Low)

1. Dong Zhang (highest difficulty, integration owner)
   - Scope:
     - OAuth2/JWT auth flow (`/auth/login`, `/auth/refresh`)
     - security policy (`401/403`, admin route protection)
     - cross-module integration and merge control
     - final backend packaging and release check
   - Deliverables:
     - stable auth/security layer with tests
     - integration checklist and final merge approval

2. Diego Romero-Lovo De la Flor
   - Scope:
     - landmarks and routes domain API
     - admin CRUD for landmarks/routes
     - request validation and error contract alignment
   - Deliverables:
     - `/landmarks`, `/routes`, `/admin/landmarks`, `/admin/routes`
     - API responses aligned with `openapi/nw-trails-v1.yaml`

3. Zhi Kang
   - Scope:
     - check-in business rules (50m distance, duplicate-per-day)
     - active route progress advancement and completion logic
     - progress endpoints for badge/category status
   - Deliverables:
     - `/checkins`, `/progress/me`, `/routes/{routeId}/start`
     - test coverage for out-of-range and duplicate check-in rules

4. Wangmenghua
   - Scope:
     - Swagger/OpenAPI consistency review and annotation cleanup
     - integration report draft and API evidence collection
     - smoke test recording support and submission artifact checks
   - Deliverables:
     - finalized API docs and report sections
     - submission readiness checklist (naming/package correctness)

## GitHub Issue Mapping

- `B1-auth-security-integration` -> Dong Zhang
- `B2-landmarks-routes-crud` -> Diego Romero-Lovo De la Flor
- `B3-checkin-progress-engine` -> Zhi Kang
- `B4-swagger-reporting-submission` -> Wangmenghua

## Dependency Order

1. B1 baseline security available
2. B2 and B3 implementation in parallel
3. B4 verifies docs/report using merged B1-B3 endpoints
4. Dong runs final integration gate and release packaging

## Done Criteria

- `mvn test` passes
- Swagger endpoints match `openapi/nw-trails-v1.yaml`
- Flutter app can complete at least 7 user stories against backend
- Submission artifacts prepared:
  - `groupXX-final.docx`
  - `groupXX-app.zip`
  - `groupXX-backend.zip`
  - bundled as `groupXX-project02.zip`
