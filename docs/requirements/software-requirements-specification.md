# SRM Platform — Software Requirements Specification (SRS)

**Document ID:** SRM-SRS-001  
**Version:** 1.0  
**Status:** Draft for review  
**Date:** 21 November 2026  
**Product:** Independent Cloud-Based Supplier Relationship Management Platform  
**Repository:** `Mr-Rahat-Hacker/srm-erp`

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) defines the functional and non-functional requirements for the first release of the SRM Platform. It is the baseline for use-case modelling, activity diagrams, database design, API design, implementation and testing.

### 1.2 Product scope

The product is an independent cloud-based Supplier Relationship Management application. It digitizes supplier onboarding, qualification, RFQ, quotation submission, commercial evaluation, negotiation, approval, award, notifications and audit workflows.

The first release is an SRM application, not a full Procurement ERP. It complements SAP in complex manufacturing environments. SAP remains the system of record for SAP Vendor Master / Business Partner and Material Master, and remains responsible for procurement execution.

### 1.3 Intended audience

- Product owner and project stakeholders
- Procurement and supplier-management users
- Supplier organizations using the supplier portal
- Software developers and solution architects
- QA / test engineers
- Auditors and security reviewers

### 1.4 Definitions

| Term | Meaning |
|---|---|
| SRM | Supplier Relationship Management |
| RFQ | Request for Quotation |
| PIR | Purchase Info Record |
| SAP Vendor Code | Existing SAP supplier/vendor identifier mapped into SRM |
| RBAC | Role-Based Access Control |
| UOM | Unit of Measure |
| Audit event | Append-only record of a relevant security or business action |
| Supplier organization | A supplier company represented in the SRM platform |
| Quotation version | A separately retained submission/revision of a supplier quotation |

### 1.5 References

- `docs/requirements/mvp-scope.md`
- `docs/requirements/srm-workflow.md`
- `docs/requirements/rbac-role-permission-matrix.md`
- `docs/requirements/audit-log-specification.md`
- `docs/architecture/overview.md`
- SRM Product Vision & Roadmap, Version 1.0, 21 November 2026

---

## 2. Overall description

### 2.1 Product perspective

The platform uses a modular-monolith architecture for the first release:

- Angular supplier portal and internal buyer / verifier / approver / administrator portal
- Java and Spring Boot REST API
- PostgreSQL database for SRM-owned data
- Spring Security with JWT-based authentication and backend-enforced RBAC
- SMTP / JavaMailSender and in-application notifications
- A future SAP integration adapter using authorized SAP APIs or integration middleware

### 2.2 Product boundaries and SAP ownership

1. SRM creates and manages its own internal Supplier ID.
2. SAP remains the owner of SAP Vendor Master / Business Partner and Material Master.
3. SRM may store a mapping to an existing SAP Vendor Code after the authorized SAP process creates the vendor.
4. RFQ line items refer to existing SAP Material Codes and related reference information.
5. SRM must not create or maintain SAP Material Master or SAP Vendor Master.
6. The final initial-SRM integration milestone is controlled creation or update of SAP Purchase Info Records (PIR) and relevant purchasing rates / conditions through authorized SAP APIs or integration.
7. SAP writes require authorization, validation, approval where configured, logging and traceability. Direct SAP database writes are prohibited.

### 2.3 User classes and roles

The first release defines seven roles:

| Role | Main responsibility |
|---|---|
| `SUPER_ADMIN` | Platform governance, global security settings, role/permission definitions, SRM administrator governance and emergency administration |
| `SRM_ADMIN` | Day-to-day application administration and operational support within delegated permissions |
| `SUPPLIER_USER` | Manages own supplier organization profile, documents, invited RFQs, quotations and negotiation responses |
| `SUPPLIER_VERIFIER` | Reviews supplier registration and documents; requests corrections and records verification / qualification outcomes |
| `PROCUREMENT_BUYER` | Creates RFQs, invites qualified suppliers, evaluates quotations, negotiates and recommends awards |
| `PROCUREMENT_APPROVER` | Reviews and decides only on approval tasks assigned to the user or authorized approval group |
| `AUDITOR` | Read-only access to authorized business records, decisions and audit history |

A user may hold multiple roles only where organizational policy permits. Conflicting responsibilities must be controlled through segregation of duties. Exact delegation and organizational scoping are configurable design decisions to be confirmed.

### 2.4 Operating environment

- Cloud-hosted application accessed through a modern web browser.
- Angular frontend communicates with Spring Boot REST APIs over HTTPS.
- PostgreSQL stores SRM-owned application data.
- Development environment is GitHub Codespaces with containerized services.
- SAP connectivity is introduced only through an approved integration layer and authorized SAP interfaces.

### 2.5 Constraints

- Backend authorization is authoritative; frontend route guards are supplementary.
- Supplier data must be isolated by supplier organization.
- Submitted quotation versions and audit events must not be silently overwritten or deleted.
- Approval decisions must be made inside the authenticated application.
- Secrets must not be committed to source control or written to audit logs.
- The first release must not expand into excluded procurement execution functions.

### 2.6 Assumptions and dependencies

- Supplier organizations can provide accurate company information and required documents.
- Internal stakeholders define document types and qualification criteria.
- Procurement stakeholders define RFQ fields, commercial evaluation criteria and approval policies.
- Email delivery depends on configured SMTP service.
- SAP PIR / purchasing condition integration depends on availability and authorization of SAP APIs or middleware.
- Applicable document retention, privacy and audit-retention policies will be confirmed before production deployment.

---

## 3. Functional requirements

Requirement IDs are stable references for use cases, design and test cases. Unless marked otherwise, requirements are mandatory for the first release.

### 3.1 Identity and access management

- **FR-IAM-001:** The system shall authenticate users before granting access to protected functions.
- **FR-IAM-002:** The system shall support the seven roles defined in Section 2.3.
- **FR-IAM-003:** The backend shall enforce permissions using deny-by-default RBAC.
- **FR-IAM-004:** The system shall associate each supplier user with a supplier organization.
- **FR-IAM-005:** The system shall enforce resource-level authorization for supplier ownership, RFQ invitations, quotation ownership and approval-task assignment.
- **FR-IAM-006:** A supplier user shall access only its own organization's profile, documents, invitations, quotations and negotiation responses.
- **FR-IAM-007:** A supplier user shall not view another supplier's quotation, bid or negotiation messages.
- **FR-IAM-008:** An approver shall act only on tasks assigned to that user or an authorized approval group.
- **FR-IAM-009:** The system shall support account activation, suspension and deactivation.
- **FR-IAM-010:** The system shall record user and role assignment changes in the audit trail.
- **FR-IAM-011:** SRM Admin permissions shall be limited to delegated administrative functions and shall not automatically grant business approval authority.
- **FR-IAM-012:** Super Admin shall have separately controlled platform-governance privileges. Super Admin shall not silently modify quotations, negotiation records, approval decisions or award outcomes.
- **FR-IAM-013:** Auditor access shall be read-only.
- **FR-IAM-014:** Authentication shall use secure password hashing and a defined JWT expiry, refresh and revocation lifecycle.
- **FR-IAM-015:** The system shall record relevant successful and denied security-sensitive actions without logging credentials, tokens or secrets.

### 3.2 Supplier registration and profile

- **FR-SUP-001:** The system shall allow a supplier organization to self-register.
- **FR-SUP-002:** The system shall capture configured company profile and address information.
- **FR-SUP-003:** The system shall capture supplier contact details.
- **FR-SUP-004:** The system shall assign an internal unique Supplier ID.
- **FR-SUP-005:** The system shall allow an authorized supplier user to update permitted profile fields.
- **FR-SUP-006:** The system shall maintain supplier lifecycle status and status history.
- **FR-SUP-007:** The system shall support multiple supplier users per organization only if enabled by the approved account policy.

### 3.3 Supplier document management

- **FR-DOC-001:** The system shall allow suppliers to upload configured documents, including GST, PAN and certifications.
- **FR-DOC-002:** The system shall store document metadata, including document type, upload time, uploader and current review status.
- **FR-DOC-003:** The system shall restrict document access by authentication, role and supplier-organization ownership.
- **FR-DOC-004:** The system shall allow authorized verifiers to review submitted documents.
- **FR-DOC-005:** The system shall support document correction / replacement while retaining relevant revision history.
- **FR-DOC-006:** The system shall record document upload, replacement and verification events in the audit trail.
- **FR-DOC-007:** The system shall validate configured file types and size limits. Exact allowed formats and limits shall be defined during detailed design.

### 3.4 Supplier verification and qualification

- **FR-QUAL-001:** The system shall provide an internal queue of supplier registrations awaiting review.
- **FR-QUAL-002:** An authorized Supplier Verifier shall review supplier profile information and documents.
- **FR-QUAL-003:** A verifier shall be able to request corrections with a recorded reason or comment.
- **FR-QUAL-004:** A supplier shall be able to resubmit corrected information and documents.
- **FR-QUAL-005:** An authorized verifier shall be able to qualify / approve or reject a supplier registration with a recorded reason.
- **FR-QUAL-006:** Only qualified suppliers shall be eligible for RFQ invitation, subject to buyer authorization.
- **FR-QUAL-007:** The system shall support supplier suspension and preserve the reason and status history.
- **FR-QUAL-008:** The system shall allow mapping of an existing SAP Vendor Code to an SRM Supplier ID by an authorized internal user.
- **FR-QUAL-009:** The system shall not create SAP Vendor Master / Business Partner records.

### 3.5 RFQ management

- **FR-RFQ-001:** An authorized Procurement Buyer shall be able to create an RFQ.
- **FR-RFQ-002:** The RFQ shall support a unique RFQ number, title, scope, required-by deadline and configured commercial terms.
- **FR-RFQ-003:** An RFQ shall support one or more line items.
- **FR-RFQ-004:** Each line item shall support an existing SAP Material Code reference, description, quantity, UOM and configured plant / reference information.
- **FR-RFQ-005:** The system shall not create or maintain SAP Material Master records.
- **FR-RFQ-006:** A buyer shall be able to select and invite multiple qualified suppliers.
- **FR-RFQ-007:** Only invited supplier users shall access the corresponding supplier-facing RFQ.
- **FR-RFQ-008:** The system shall track RFQ status, invitation status, deadline and relevant communications.
- **FR-RFQ-009:** Authorized users shall be able to modify or cancel an RFQ according to its lifecycle permissions; material changes after publication shall be traceable and communicated to invited suppliers.
- **FR-RFQ-010:** The system shall prevent unauthorized access to RFQs outside a user's business scope.

### 3.6 Quotation and bidding

- **FR-QUO-001:** An invited supplier shall be able to prepare a quotation against the relevant RFQ.
- **FR-QUO-002:** The quotation shall support line-level price and configured currency, taxes / charges, delivery lead time, validity and commercial remarks.
- **FR-QUO-003:** The system shall validate required quotation fields before final submission.
- **FR-QUO-004:** The system shall validate submission against the RFQ deadline and configured submission rules.
- **FR-QUO-005:** The system shall record supplier identity and server-side submission timestamp.
- **FR-QUO-006:** A submitted quotation shall be retained as a version; later revisions shall create new versions rather than silently overwrite prior submissions.
- **FR-QUO-007:** The system shall support authorized withdrawal or resubmission according to RFQ rules and preserve history.
- **FR-QUO-008:** Suppliers shall access only their own quotations and submission history.
- **FR-QUO-009:** The system shall record quotation submission, revision and withdrawal events in the audit trail.

### 3.7 Quotation comparison and commercial evaluation

- **FR-EVAL-001:** An authorized buyer shall be able to compare quotations for an RFQ.
- **FR-EVAL-002:** The comparison shall support line-by-line comparison of quoted price and configured commercial fields.
- **FR-EVAL-003:** The comparison shall support total commercial value, delivery lead time and quotation validity where data is available.
- **FR-EVAL-004:** The system shall identify missing or non-comparable quotation information.
- **FR-EVAL-005:** An authorized buyer shall be able to record evaluation remarks and recommendation.
- **FR-EVAL-006:** The system shall retain evaluation history and identify the user and time of changes.
- **FR-EVAL-007:** The system shall not automatically select a supplier solely on lowest price unless a future approved policy explicitly defines that behavior.

### 3.8 Negotiation

- **FR-NEG-001:** An authorized buyer shall be able to initiate negotiation with participating suppliers according to RFQ policy.
- **FR-NEG-002:** The system shall record negotiation round, message / request, response deadline and participants.
- **FR-NEG-003:** An invited supplier shall be able to respond only to its own negotiation request.
- **FR-NEG-004:** A supplier's revised quotation shall be stored as a new quotation version.
- **FR-NEG-005:** The system shall preserve previous quotation versions and negotiation messages.
- **FR-NEG-006:** The system shall support comparison of commercial changes between quotation versions.
- **FR-NEG-007:** An authorized buyer shall be able to close a negotiation round and record its outcome.
- **FR-NEG-008:** Negotiation actions and messages shall be included in the audit trail.

### 3.9 Recommendation, approval and award

- **FR-APR-001:** An authorized buyer shall be able to record a supplier recommendation for an RFQ.
- **FR-APR-002:** A recommendation shall include commercial justification and relevant evaluation context.
- **FR-APR-003:** The system shall route an approval request according to configured approval workflow rules.
  - Award policies match the approved quotation total landed cost in the RFQ currency to one non-overlapping amount range: minimum inclusive, maximum exclusive (or unbounded). A matching policy supplies an ordered sequence of 1–10 active procurement approvers; the requester and recommender remain excluded from approval. A request without exactly one matching policy shall be blocked, and the applied policy code, currency, amount and route shall be audited.
- **FR-APR-004:** An approver shall be able to review the RFQ, quotation comparison, negotiation history and buyer justification within the authenticated application.
- **FR-APR-005:** An assigned approver shall be able to approve, reject or return an approval request for revision.
- **FR-APR-006:** Each approval decision shall record actor, timestamp, decision and comments / reason.
- **FR-APR-007:** By default, the user who made the supplier recommendation shall not approve the same award. Any policy exception shall be explicitly configured and audited.
- **FR-APR-008:** Email notifications shall not provide an unauthenticated approval mechanism.
- **FR-AWD-001:** An authorized buyer shall record the award only after the required approval is complete.
- **FR-AWD-002:** The award record shall identify the RFQ, selected supplier, approved quotation / version and decision reference.
- **FR-AWD-003:** The system shall notify the selected and unsuccessful suppliers according to configured notification rules.
- **FR-AWD-004:** Award cancellation or reversal, if permitted by policy, shall require authorization, reason and audit history.

### 3.10 Notifications and communication

- **FR-NOT-001:** The system shall provide in-application notifications for relevant workflow events.
- **FR-NOT-002:** The system shall send email notifications for configured events such as registration updates, correction requests, RFQ invitations, negotiation requests, approval tasks and award outcomes.
- **FR-NOT-003:** The system shall record notification status, including queued, sent, failed and retried where available.
- **FR-NOT-004:** Failed notifications shall support controlled retry handling.
- **FR-NOT-005:** Notification links shall require normal authentication and authorization.
- **FR-NOT-006:** Notification failure shall not silently change or reverse a business decision.

### 3.11 Audit trail

- **FR-AUD-001:** The system shall maintain an append-only audit trail for important security and business events.
- **FR-AUD-002:** Audit events shall include unique event ID, UTC timestamp, actor, effective role(s), actor type, action, target entity, target ID and outcome where applicable.
- **FR-AUD-003:** Audit events shall include supplier organization, RFQ, quotation or approval correlation references where applicable.
- **FR-AUD-004:** Audit events shall capture reasons, justifications and changed-field summaries where required, without recording secrets or unnecessary sensitive document contents.
- **FR-AUD-005:** Normal application users and administrators shall not edit or delete historical audit events.
- **FR-AUD-006:** Authorized users shall be able to search audit events using time, actor, role, action, entity, supplier, RFQ, quotation, approval and outcome filters, subject to access scope.
- **FR-AUD-007:** Auditor shall have read-only access to authorized audit records.
- **FR-AUD-008:** Super Admin shall have read-only audit access by default; privileged access attempts shall themselves be recorded.
- **FR-AUD-009:** Audit searches and exports shall be logged where appropriate.
- **FR-AUD-010:** The system shall capture relevant authentication, role changes, supplier lifecycle, RFQ, quotation, negotiation, evaluation, approval, award, notification and SAP integration events.
- **FR-AUD-011:** Audit events for critical business state changes shall be persisted reliably, transactionally where feasible.
- **FR-AUD-012:** Audit logs shall not contain passwords, access tokens, private keys or full sensitive document contents.

### 3.12 Dashboards and reports

- **FR-REP-001:** The system shall provide role-appropriate dashboards.
- **FR-REP-002:** Internal users shall be able to view relevant supplier onboarding and qualification statuses.
- **FR-REP-003:** Authorized procurement users shall be able to view active RFQs, quotation status, negotiation status, pending approvals and awards.
- **FR-REP-004:** Report data shall respect RBAC and supplier-organization isolation.
- **FR-REP-005:** Any export function shall enforce the same authorization scope as on-screen data.

### 3.13 SAP integration — final initial-SRM milestone

- **FR-SAP-001:** The system shall store an authorized mapping between an SRM Supplier ID and an existing SAP Vendor Code.
- **FR-SAP-002:** The system shall use existing SAP Material Codes as RFQ references.
- **FR-SAP-003:** After the required SRM approval and award conditions are satisfied, the system shall support a controlled request to create or update an SAP Purchase Info Record (PIR) and relevant purchasing rates / conditions.
- **FR-SAP-004:** SAP integration shall use authorized SAP APIs or an approved integration layer; direct SAP database writes are prohibited.
- **FR-SAP-005:** The integration request shall be validated for required references, authorization and business rules before submission.
- **FR-SAP-006:** The system shall record request reference, response, outcome, errors and reconciliation status without exposing credentials or secrets.
- **FR-SAP-007:** The system shall prevent duplicate or unintended SAP writes through suitable idempotency / duplicate-control design.
- **FR-SAP-008:** Failed SAP requests shall be visible to authorized users for controlled correction and retry.
- **FR-SAP-009:** SAP integration activity shall be included in the audit trail.
- **FR-SAP-010:** The initial release shall not create SAP Vendor Master / Business Partner, Material Master, PR, PO, GRN or invoice records and shall not execute MRP or Schedule Agreements.

---

## 4. Business rules

- **BR-001:** Only qualified suppliers may be invited to an RFQ, unless an explicitly approved exception policy is introduced.
- **BR-002:** A supplier may view only RFQs for which its organization has a valid invitation.
- **BR-003:** A supplier may view and revise only its own quotation.
- **BR-004:** Final quotation submission must follow the RFQ deadline and submission policy.
- **BR-005:** A submitted quotation revision is a new version; prior submitted versions remain traceable.
- **BR-006:** Buyer recommendation and approver decision are separate responsibilities by default.
- **BR-007:** Only an authorized approver may decide an assigned approval task.
- **BR-008:** Award recording is permitted only after required approval.
- **BR-009:** Supplier award notifications must reflect the recorded award outcome.
- **BR-010:** SAP Vendor and Material Master remain SAP-owned.
- **BR-011:** SAP PIR / purchasing condition updates require authorized integration, validation and traceability.
- **BR-012:** Audit history is append-only through normal application workflows.

---

## 5. Data requirements — logical entities

The following entities are logical requirements for subsequent ER modelling. This section does not prescribe final table names or physical schema.

| Entity | Purpose |
|---|---|
| User | Authenticated platform identity |
| Role | Named platform role |
| Permission | Atomic authorization capability |
| UserRole | User-to-role assignment |
| Supplier | SRM supplier organization and internal Supplier ID |
| SupplierContact | Supplier contact details |
| SupplierDocument | Document metadata, storage reference and review status |
| SupplierDocumentVersion | Retained document revision metadata |
| SupplierVerification | Verification review, correction and outcome history |
| SupplierSapMapping | Existing SAP Vendor Code mapping |
| RFQ | RFQ header, deadline, scope, terms and lifecycle status |
| RFQLine | SAP Material Code reference, description, quantity, UOM and plant/reference |
| RFQInvitation | Supplier invitation and access status |
| Quotation | Supplier quotation identity and current lifecycle |
| QuotationVersion | Immutable submitted quotation version and commercial values |
| QuotationLine | Quoted values for an RFQ line |
| Evaluation | Buyer commercial evaluation and remarks |
| NegotiationRound | Negotiation round and deadline |
| NegotiationMessage | Buyer / supplier negotiation communication |
| ApprovalRequest | Approval workflow instance and target business record |
| ApprovalTask | Assigned approver or group task |
| ApprovalDecision | Approve / reject / return decision and comments |
| Award | Approved supplier award record |
| Notification | In-app / email notification record and delivery status |
| AuditEvent | Append-only security and business event |
| SapIntegrationRequest | Controlled SAP mapping / PIR / rate-condition integration request and result |

All relationships, cardinalities, constraints and normalization decisions shall be finalized in the PostgreSQL ER Diagram and data dictionary milestone.

---

## 6. External interface requirements

### 6.1 User interface

- **UI-001:** Supplier-facing screens shall expose only the supplier organization's permitted profile, documents, RFQs, quotations and negotiation actions.
- **UI-002:** Internal navigation and actions shall be permission-aware.
- **UI-003:** Buyer screens shall support RFQ creation, invitations, quotation comparison, negotiation, recommendation and award recording.
- **UI-004:** Approver screens shall show assigned approval tasks and the context needed for a decision.
- **UI-005:** Auditor screens shall provide read-only search and review of authorized records.
- **UI-006:** Administrative screens shall separate SRM Admin operational functions from Super Admin platform governance.
- **UI-007:** User-interface controls shall not be treated as a replacement for backend authorization.

### 6.2 Software interfaces

- Angular frontend shall communicate with Spring Boot REST APIs.
- REST APIs shall be versioned under `/api/v1`.
- API payloads shall use JSON unless a specific file-upload interface is required.
- PostgreSQL shall persist SRM-owned records.
- SMTP shall be used for configured email delivery.
- SAP integration shall use authorized APIs or an approved middleware / integration layer.

### 6.3 Communication interfaces

- Production client-to-server communication shall use HTTPS.
- Authentication credentials and tokens shall not be exposed in URLs or logs.
- File uploads and downloads shall use authenticated, authorized access paths.

---

## 7. Non-functional requirements

The following are initial quality requirements. Numeric service-level targets must be agreed before production deployment.

### 7.1 Security

- **NFR-SEC-001:** All protected API operations shall require authentication.
- **NFR-SEC-002:** Backend authorization shall enforce least privilege and deny by default.
- **NFR-SEC-003:** Supplier organization isolation shall be enforced on every relevant read and write.
- **NFR-SEC-004:** Passwords shall be stored using an industry-standard adaptive password-hashing algorithm.
- **NFR-SEC-005:** JWT expiry, refresh, revocation and signing-key management shall be defined securely.
- **NFR-SEC-006:** Sensitive data shall be protected in transit using HTTPS and at rest according to deployment policy.
- **NFR-SEC-007:** Uploaded files shall be validated and access-controlled.
- **NFR-SEC-008:** Secrets and credentials shall be supplied through secure environment / secret management, not source control.
- **NFR-SEC-009:** Sensitive security and business actions shall be audited without logging secrets.
- **NFR-SEC-010:** Automated tests shall verify both allowed and denied access, including cross-supplier access attempts.

### 7.2 Performance and scalability

- **NFR-PER-001:** Common list screens shall support server-side pagination and filtering.
- **NFR-PER-002:** Database queries shall use suitable indexes for common search and relationship fields.
- **NFR-PER-003:** Notification delivery shall not unnecessarily block core business transactions.
- **NFR-PER-004:** Audit recording shall not introduce material delays into critical workflows.
- **NFR-PER-005:** Performance targets and expected concurrent-user volumes shall be defined before production load testing.

### 7.3 Availability and recovery

- **NFR-AVL-001:** Production deployment shall include monitored application and database health.
- **NFR-AVL-002:** PostgreSQL data and uploaded documents shall be included in a defined backup strategy.
- **NFR-AVL-003:** Recovery Point Objective (RPO) and Recovery Time Objective (RTO) shall be agreed before production.
- **NFR-AVL-004:** Failed notification and SAP integration operations shall be observable and recoverable through controlled retry / reconciliation.

### 7.4 Reliability and data integrity

- **NFR-REL-001:** Business state transitions shall be validated on the server.
- **NFR-REL-002:** Critical state changes and corresponding audit events shall be persisted reliably.
- **NFR-REL-003:** Quotation versions and approval decisions shall retain historical traceability.
- **NFR-REL-004:** SAP integration requests shall use duplicate-control / idempotency safeguards.
- **NFR-REL-005:** Errors shall be reported consistently without exposing stack traces, secrets or internal implementation details to end users.

### 7.5 Maintainability and architecture

- **NFR-MNT-001:** The first release shall use a modular-monolith structure with clear module boundaries.
- **NFR-MNT-002:** Backend APIs shall follow consistent naming, validation, error and pagination conventions.
- **NFR-MNT-003:** Database changes shall be version-controlled through migrations.
- **NFR-MNT-004:** Automated unit and integration tests shall cover critical business rules and security controls.
- **NFR-MNT-005:** Configuration shall be environment-based to support reproducible cloud development and deployment.

### 7.6 Usability and compatibility

- **NFR-USE-001:** The application shall provide separate, role-appropriate experiences for suppliers and internal users.
- **NFR-USE-002:** Validation errors shall explain the corrective action in clear language.
- **NFR-USE-003:** The web interface shall support current major desktop browsers; exact supported versions shall be confirmed.
- **NFR-USE-004:** Core workflows shall display clear status, ownership and next-action information.

### 7.7 Audit and compliance readiness

- **NFR-AUD-001:** Audit events shall use UTC timestamps and consistent event naming.
- **NFR-AUD-002:** Normal application workflows shall not permit audit event modification or deletion.
- **NFR-AUD-003:** Audit access and export shall be permission-controlled and traceable.
- **NFR-AUD-004:** Retention, archival, legal hold and export policies shall be defined before production.

---

## 8. Lifecycle and status requirements

The following status values are initial proposals from the workflow baseline. Exact transition guards and permitted actors shall be finalized in UML activity / state diagrams.

### 8.1 Supplier lifecycle

`Draft → Submitted → Under Review → Correction Required → Resubmitted → Qualified / Rejected → Active / Suspended`

### 8.2 RFQ lifecycle

`Draft → Approval Pending (if configured) → Published / Invitations Sent → Quotation Open → Evaluation → Negotiation → Approval Pending → Awarded / Closed / Cancelled`

### 8.3 Quotation lifecycle

`Draft → Submitted → Under Evaluation → Negotiation Requested → Revised (versioned) → Accepted / Not Selected / Withdrawn`

### 8.4 Approval lifecycle

`Pending → Approved / Rejected / Returned for Revision`

A status change shall be validated against the current state, actor permission and business rules, and recorded in status history / audit trail.

---

## 9. Acceptance criteria

The first release shall not be considered functionally acceptable until the following end-to-end scenarios pass:

1. A supplier can register, submit profile information and upload required documents.
2. A verifier can review the registration, request corrections, and later qualify or reject the supplier with a reason.
3. An authorized user can map an existing SAP Vendor Code to the qualified SRM supplier without creating an SAP vendor record.
4. A buyer can create an RFQ using existing SAP Material Code references and invite multiple qualified suppliers.
5. Each invited supplier can access its invitation and submit a quotation before the deadline.
6. A supplier cannot access another supplier's RFQ invitation, quotation or negotiation messages.
7. Submitted quotation revisions are retained as separate versions with timestamps.
8. A buyer can compare quotations and record evaluation remarks and recommendation.
9. Buyer and supplier can complete negotiation rounds while preserving all messages and quotation versions.
10. An assigned approver can review the complete context and approve, reject or return the request inside the authenticated application.
11. The recommender cannot approve the same award by default.
12. A buyer can record an award only after required approval, and the system can notify selected and unsuccessful suppliers.
13. Authorized users can search audit events; unauthorized users cannot modify or delete audit history.
14. Super Admin can perform permitted platform-governance actions, and such actions are audited; Super Admin cannot silently change commercial or approval history.
15. Failed email and SAP integration operations are visible and can be handled through controlled retry / reconciliation.
16. SAP PIR / purchasing rate-condition integration can be executed only through authorized interfaces and only after required controls.
17. The system does not create SAP Vendor Master, Material Master, PR, PO, GRN or invoice records and does not execute MRP or Schedule Agreements.

---

## 10. Out of scope for the first release

The following are explicitly excluded:

- SAP Vendor Master / Business Partner creation or maintenance
- SAP Material Master creation or maintenance
- Purchase Requisition creation or approval
- Purchase Order creation or execution
- Goods Receipt / GRN
- Invoice verification
- MRP
- Schedule Agreement execution / delivery scheduling
- Full procurement ERP replacement
- Broader procurement capabilities reserved for a future product phase

---

## 11. Open decisions requiring stakeholder confirmation

These items are intentionally left open rather than assumed:

1. Can one supplier organization have multiple supplier users and a supplier organization administrator?
2. Which supplier document types are mandatory, and what are their validity / expiry rules?
3. What are the exact supplier qualification criteria and who can approve exceptions?
4. Are buyer permissions global or scoped by purchasing organization, plant, commodity/category or business unit?
5. Are approval tasks assigned to individuals, groups or both?
6. What delegation and out-of-office approval rules are permitted?
7. What approval thresholds and approval levels are required for award decisions?
8. Can an RFQ deadline be extended after publication, and what supplier acknowledgement is required?
9. Which commercial fields are mandatory for quotation comparison (currency, tax, freight, tooling, payment terms, delivery, validity, etc.)?
10. What are the permitted quotation withdrawal and post-deadline exception rules?
11. Which document storage service and malware-scanning controls will be used?
12. What MFA policy is required for internal users, approvers and Super Admin?
13. What are the audit retention, archival, legal hold and export requirements?
14. Which SAP S/4HANA release, API / middleware, authentication method and PIR / condition types are in scope for integration?
15. What SAP-side validation, approval, duplicate prevention and error-reconciliation rules apply to PIR / rate-condition updates?
16. What production availability, response-time, concurrent-user, RPO and RTO targets are required?
17. Which dashboard metrics and report export formats are required?

---

## 12. Next development milestone

After stakeholder review and approval of this SRS:

1. Create the standard UML Use Case Diagram covering all seven roles and first-release modules.
2. Create Activity Diagrams for Supplier Onboarding and RFQ-to-Award.
3. Create supporting State Machine Diagrams for Supplier, RFQ, Quotation and Approval lifecycles.
4. Create the PostgreSQL ER Diagram and data dictionary.
5. Finalize API contracts, security controls and test traceability.
6. Begin incremental implementation only after the requirements and data model are baselined.

**Document status:** Draft for review. Open decisions in Section 11 must be resolved or explicitly accepted as deferred before implementation of the affected behavior.
