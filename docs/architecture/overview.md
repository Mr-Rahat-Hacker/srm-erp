# SRM Platform — Architecture Overview

## 1. Architecture style

Use a modular monolith for the first SRM release. Maintain clear module boundaries so future procurement capabilities can be added without rewriting the SRM core.

## 2. High-level components

```text
Supplier Portal                 Buyer / Approver Portal
      |                                  |
      +------------ Angular ------------+
                       |
                 HTTPS / REST
                       |
               Spring Boot API
                       |
     +-----------------+----------------------+
     |          |           |          |       |
  Supplier   Sourcing   Quotation  Workflow  Notification
  Management / RFQ      & Bidding  & Approval & Audit
     |          |           |          |       |
     +---------------- PostgreSQL ------------+
                       |
               SAP Integration Adapter
                       |
             Authorized SAP APIs / Layer
```

## 3. First-release backend modules

- identity-access: users, roles, permissions and authenticated sessions.
- supplier: supplier self-registration, profile and contacts.
- supplier-documents: document upload, metadata, verification and revision history.
- supplier-qualification: verification, correction requests, approval, rejection and qualification status.
- sourcing-rfq: RFQ header, RFQ line references, invited suppliers and deadlines.
- quotation: supplier bid submission, validation and immutable quotation versions.
- evaluation: commercial comparison, evaluation criteria and buyer recommendation.
- negotiation: negotiation rounds, messages, deadlines and revised quotation versions.
- approval: configurable approval routes and recorded decisions.
- award: final award decision and supplier notifications.
- notification: email / in-app messages, delivery logs and retry handling.
- audit: traceable business activity and status history.
- integration-sap: future authorized SAP mapping and PIR / purchasing condition integration.

## 4. SAP boundary

SAP remains the system of record for SAP Vendor Master / Business Partner and Material Master and remains the execution platform for complex manufacturing procurement.

SRM stores its internal Supplier ID and maps an existing SAP Vendor Code. RFQ items refer to SAP Material Codes; SRM does not create Material Master.

The final initial-SRM integration milestone is controlled SAP PIR and relevant purchasing rate/condition creation or update through authorized APIs / integration. Do not write directly to the SAP database.

PR, PO, GRN, invoice verification, MRP and Schedule Agreement execution are outside the initial SRM release.

## 5. Data ownership

PostgreSQL stores SRM-owned workflow data, supplier-submitted documents and quotations, negotiation versions, approval decisions, audit events and notifications. SAP-owned master data remains in SAP; store references and mappings only as required.

## 6. API conventions

- Version APIs under `/api/v1`.
- Use JSON request / response bodies.
- Validate all supplier and buyer input server-side.
- Use pagination and filtering for collections.
- Enforce authorization on every protected operation.
- Preserve quotation and approval history; avoid destructive overwrites.
- Use consistent error response format.

## 7. Security baseline

- Spring Security and JWT authentication.
- Role-based access control for supplier, buyer, approver, administrator and auditor.
- Suppliers can access only their own profile, invited RFQs and submitted quotations.
- Buyers can access RFQs and supplier submissions within their authorization.
- Approvers can act only on assigned approval tasks.
- Protect uploaded documents and restrict access by role.
- Keep secrets outside source control.
- Audit sensitive actions and SAP integration events.

## 8. Notifications and audit

Use SMTP / JavaMailSender and in-app notifications with delivery logs and retry handling. Notification links must not bypass authentication or directly approve transactions. Approval decisions are made inside the authenticated application.

## 9. Deployment

Develop in GitHub Codespaces. Keep Angular, Spring Boot and PostgreSQL configuration reproducible. Use environment-based configuration and containerized local development.
