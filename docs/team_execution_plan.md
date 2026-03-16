# Team Execution Plan (Backend)

## Branching

- Main branch: `main` (protected, PR only)
- Feature branches:
  - `feat/auth-oauth2`
  - `feat/landmarks-routes-crud`
  - `feat/checkin-progress`
  - `feat/swagger-reporting`

## Task Split

1. Auth + Security owner
   - Harden JWT flow and roles
   - Add authorization tests (`401/403`)
2. Domain API owner
   - Landmarks/routes CRUD persistence layer
   - Validation and error contract alignment
3. Check-in owner
   - 50m proximity verification integration
   - Duplicate-per-day guard and progress state
4. Integration owner
   - Flutter API client wiring (remove app stubs)
   - End-to-end smoke tests + report assembly

## Done Criteria

- `mvn test` passes
- Swagger endpoints match `openapi/nw-trails-v1.yaml`
- Flutter app can complete at least 7 user stories against backend
- Submission artifacts prepared:
  - `groupXX-final.docx`
  - `groupXX-app.zip`
  - `groupXX-backend.zip`
  - bundled as `groupXX-project02.zip`
