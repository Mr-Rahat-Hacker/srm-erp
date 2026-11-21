# SRM Development — Phase 1 Foundation

## Objective

Establish consistent API conventions and the first Angular application shell before implementing business workflows.

## Backend baseline

- Spring Boot 3.5.6, Java 21 and PostgreSQL.
- REST API version prefix: `/api/v1`.
- `ApiResponse<T>` is the standard success envelope for new business endpoints.
- `ApiErrorResponse` is the standard error envelope.
- `GlobalExceptionHandler` centralizes request validation, unreadable request and unexpected error responses.
- Do not expose exception messages, SQL details, stack traces or secrets to API consumers.
- Preserve the existing health endpoint response until clients and monitoring are migrated.

## Frontend baseline

- Angular standalone components.
- Central route configuration in `app.routes.ts`.
- Shared application shell with navigation and routed page content.
- Feature components live under `features/`; reusable UI lives under `shared/`.
- Route guards improve navigation UX only; backend authorization remains authoritative.
- The current dashboard is a development overview, not production business metrics.

## Next vertical slice

Supplier Registration:
1. Confirm supplier registration fields and mandatory validations against SRS and database schema.
2. Implement JPA entities and repositories for supplier organization, address and contact.
3. Implement DTOs, mapper, service and REST controller.
4. Add backend unit and integration tests.
5. Build Angular registration form and connect it to the API.
6. Verify persistence in PostgreSQL and test validation/error cases.

## Run locally in Codespaces

Backend (from repository root):

```bash
cd backend
mvn test
mvn spring-boot:run
```

Frontend (second terminal, from repository root):

```bash
cd frontend
npm ci
npm start -- --host 0.0.0.0
```

Expected development ports: API `8080`, Angular `4200`, PostgreSQL `5432`.

The API database URL must use a hostname reachable from the process running Spring Boot. In the workspace container on the Compose network, use `postgres`; if running directly in a Codespaces workspace without that network, configure `DB_URL` to the published PostgreSQL endpoint.
