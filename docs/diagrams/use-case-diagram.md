# UML Use Case Diagram — SRM First Release

**Diagram ID:** SRM-UML-UC-001  
**Notation:** UML 2.x Use Case Diagram  
**Status:** Draft for review  
**Source:** [use-case-diagram.puml](use-case-diagram.puml)

## 1. System boundary

The rectangle named **SRM Platform** is the system boundary. Use cases inside it are responsibilities of the SRM application. External actors interact with the system through its interfaces.

## 2. Actors

| Actor | Responsibility represented |
|---|---|
| Supplier User | Registration, own supplier profile and documents, invited RFQs, own quotations and negotiation responses |
| Supplier Verifier | Supplier document/profile review, correction requests, qualification or rejection |
| Procurement Buyer | RFQ, invitations, quotation comparison, evaluation, negotiation, recommendation, award and controlled SAP integration initiation |
| Procurement Approver | Review and decision on assigned approval tasks |
| SRM Admin | Day-to-day user and delegated application administration |
| Super Admin | Platform governance, global roles and security settings |
| Auditor | Read-only audit trail and authorized reports |
| Email Service | External delivery service for configured email notifications |
| SAP System / Integration Layer | External SAP interface for existing Vendor Code mapping and final PIR / purchasing condition integration |

## 3. Main use-case groups

1. Identity and access management
2. Supplier management and qualification
3. Sourcing, RFQ and quotation
4. Approval and award
5. Notifications, reporting, audit and SAP integration

## 4. Important modelling rules

- Supplier users are restricted to their own supplier organization and invited RFQs.
- Supplier quotation and negotiation data is private to that supplier and authorized internal roles.
- Approvers act only on assigned approval tasks.
- Recommending and approving the same award are separate duties by default.
- Auditor is read-only.
- Super Admin governs the platform; it does not silently modify commercial or approval decisions.
- SAP remains the system of record for Vendor Master / Business Partner and Material Master.
- SAP PIR / purchasing rate-condition updates are a controlled integration use case, not direct SAP database access.
- PR, PO, GRN, invoice verification, MRP and Schedule Agreement execution are outside this first-release diagram.

## 5. Traceability

The use cases are derived from:
- `docs/requirements/software-requirements-specification.md`
- `docs/requirements/rbac-role-permission-matrix.md`
- `docs/requirements/audit-log-specification.md`
- `docs/requirements/srm-workflow.md`

## 6. Review status

This is the first UML use-case baseline. Confirm actor permissions and the SAP integration boundary before treating it as approved. Activity diagrams are the next modelling step.
