# Integration Gate Checklist (B1 Owner)

Use this checklist before final backend release packaging.

## Auth and Security

- [ ] `/api/v1/auth/login` returns access and refresh tokens.
- [ ] `/api/v1/auth/refresh` rotates refresh tokens and rejects replayed tokens.
- [ ] Protected endpoints return `401` with JSON body (`code=UNAUTHORIZED`) when token is missing or invalid.
- [ ] Admin endpoints (`/api/v1/admin/**`) return `403` with JSON body (`code=FORBIDDEN`) for non-admin users.

## Cross-Module Integration

- [ ] B2 landmarks/routes APIs run against current auth policy.
- [ ] B3 check-in/progress APIs run against current auth policy.
- [ ] Smoke tests cover login -> protected read -> check-in/progress flow.

## Contract and Submission

- [ ] Error response shape follows `code/message/details` contract.
- [ ] OpenAPI (`openapi/nw-trails-v1.yaml`) matches merged endpoints.
- [ ] `mvn test` passes in CI.
- [ ] Release artifacts are prepared according to submission checklist.
