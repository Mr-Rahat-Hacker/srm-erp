# SRM ERP

Supplier Relationship Management (SRM) ERP is a cloud-developed procurement and supplier management application. It is designed as an independent system with a future SAP integration layer.

## Project goals

- Manage supplier, material and purchasing master data.
- Run RFQ and quotation comparison workflows.
- Create and track purchase requisitions and purchase orders.
- Maintain Purchase Info Records and Schedule Agreements.
- Track goods receipt, invoice status and procurement reporting.
- Provide controlled integration with SAP S/4HANA when configured.

## Planned technology stack

| Layer | Technology |
|---|---|
| Frontend | Angular |
| Backend | Java + Spring Boot |
| Database | PostgreSQL |
| API | REST / JSON |
| Development | GitHub Codespaces |
| CI | GitHub Actions |

## Repository layout

```text
docs/             Requirements, architecture and UML
frontend/         Angular application (planned)
backend/          Spring Boot API (planned)
database/         PostgreSQL schema and migrations
integrations/sap/ SAP integration contracts and adapters
tests/            Test strategy and shared test assets
.github/workflows CI workflows
```

## Development principles

1. Build the SRM application independently; SAP is an optional external integration.
2. Keep business rules in the backend, not in the Angular UI.
3. Use PostgreSQL as the system-of-record database for the SRM application.
4. Keep supplier, material, purchasing and inventory data auditable.
5. Deliver features in small, tested increments.

## Current status

**Phase 0 — Repository and architecture foundation.**

See [MVP scope](docs/requirements/mvp-scope.md) and [architecture overview](docs/architecture/overview.md).

## Local development

The initial development environment will be GitHub Codespaces. Setup instructions will be added with the first runnable application scaffold.
