# SRM ERP — Architecture Overview

## 1. Architecture style

Use a modular monolith for the MVP. This keeps deployment and development straightforward while maintaining clear module boundaries for future extraction if scale requires it.

## 2. High-level components

```text
Angular Web Client
       |
       | HTTPS / REST JSON
       v
Spring Boot Application
  |       |        |
  |       |        +-- Integration Adapters (SAP / external systems)
  |       +----------- Business Modules
  |                    Supplier, Material, Sourcing,
  |                    Purchasing, Inventory, Invoice
  |
  +---------------- PostgreSQL
```

## 3. Backend module boundaries

- identity: users, roles and permissions.
- supplier: supplier master and contacts.
- material: material master and units.
- purchasing-master: Purchase Info Records and Schedule Agreements.
- sourcing: RFQs, supplier invitations and quotations.
- procurement: PR, approvals and PO.
- inventory: GRN and receipt transactions.
- invoice: invoice references and matching status.
- integration: SAP adapters, mappings and synchronization logs.
- audit: business change history.

## 4. Data ownership

PostgreSQL is the primary database for SRM-owned records. SAP remains an external system of record for any data explicitly governed by the customer's SAP configuration. Ownership and synchronization direction must be defined per entity before enabling integration.

## 5. API conventions

- Version APIs under `/api/v1`.
- Use JSON request and response bodies.
- Validate input on the server.
- Return consistent error responses.
- Use pagination for collection endpoints.
- Protect state-changing operations with authorization and audit logging.

## 6. Security baseline

- HTTPS in deployed environments.
- Password hashing using a modern adaptive algorithm.
- Role-based authorization.
- Server-side validation.
- Secrets stored in environment variables or a secret manager, never committed.
- Audit sensitive changes and integration events.

## 7. Deployment approach

Develop in GitHub Codespaces. Keep the frontend, backend and database configuration reproducible. Container-based development will be introduced with the runnable scaffold.
