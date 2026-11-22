# SRM Platform

Independent, cloud-based Supplier Relationship Management application.

## Product vision

Digitize supplier onboarding, qualification, RFQ, quotation/bidding, commercial evaluation, negotiation, approval, award and supplier communication workflows.

**The first release is SRM, not a full Procurement ERP.** The platform complements SAP in complex manufacturing environments; it does not replace SAP master-data ownership or procurement execution.

## First-release modules

1. Supplier self-registration and company profile
2. Supplier contacts and document upload (GST, PAN, certifications)
3. Internal supplier verification, qualification, correction request, approval and rejection
4. Mapping an existing SAP Vendor Code after the authorized SAP process creates the vendor
5. RFQ creation using SAP Material Code references (no Material Master creation in SRM)
6. RFQ invitation to multiple approved suppliers and supplier portal
7. Supplier quotation / bid submission
8. Quotation comparison and commercial evaluation
9. Negotiation rounds and version-controlled revised quotations
10. Supplier recommendation, justification and configurable approval
11. Award decision and selected / unsuccessful supplier notifications
12. Email notifications, audit trail, status tracking, dashboards and reports

## Explicitly excluded from initial SRM release

- Material Master creation or maintenance
- SAP Vendor Master / Business Partner creation
- Purchase Requisition creation
- Purchase Order creation or execution
- Goods Receipt / GRN
- Invoice verification
- MRP
- Schedule Agreement execution

SAP remains responsible for SAP master data and complex manufacturing procurement execution. SRM uses SAP Material Codes as RFQ references and maps an existing SAP Vendor Code.

## Final initial-SRM integration milestone

Controlled creation or update of SAP Purchase Info Records (PIR) and relevant purchasing rates/conditions, subject to authorized SAP APIs, validations, approvals and audit logging.

## Future direction

After the SRM release is proven, the product may expand into broader cloud procurement for organizations without a complete ERP procurement process. This is future scope, not a first-release commitment.

## Security and access control

The first release includes backend-enforced Role-Based Access Control (RBAC), supplier organization data isolation, approval-task assignment checks, segregation of duties, a read-only auditor role and a separately controlled Super Admin role for platform governance. An append-only audit trail records important security and business actions. See [RBAC role and permission matrix](docs/requirements/rbac-role-permission-matrix.md) and [Audit Log Specification](docs/requirements/audit-log-specification.md).

## Technology

- Frontend: Angular
- Backend: Java + Spring Boot REST APIs
- Database: PostgreSQL
- Security: Spring Security, JWT and role-based access control
- Notifications: SMTP / JavaMailSender with logs and retry handling
- Development: GitHub Codespaces

## Repository guide

- [Software Requirements Specification (SRS)](docs/requirements/software-requirements-specification.md)
- [Product scope](docs/requirements/mvp-scope.md)
- [SRM workflow](docs/requirements/srm-workflow.md)
- [Architecture](docs/architecture/overview.md)

## Current status

The repository now contains working supplier registration and qualification, RFQ/invitation, versioned quotations, evaluation, negotiation, approval and award workflows, notifications, audit search, reporting, and a controlled SAP PIR outbox/gateway adapter. Backend tests and the Angular production build run in GitHub Actions.

Award approvals support a buyer-selected sequence of up to 10 distinct active approvers; reusable organizational policy rules and monetary thresholds remain open. Email delivery and SAP gateway calls require deployment-specific SMTP and authorized gateway configuration before live integration.
