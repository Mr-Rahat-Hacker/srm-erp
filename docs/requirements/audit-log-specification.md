# SRM Platform — Audit Log and Audit Trail Specification

## 1. Purpose

Maintain a reliable, searchable and tamper-resistant history of important SRM activities for internal audit, supplier dispute resolution, compliance review and security investigation.

## 2. Events to capture

- Authentication: successful login, failed login, logout, token/session revocation and account lockout.
- Identity and access: user creation/deactivation, role assignment/removal, permission changes and privileged Super Admin actions.
- Supplier lifecycle: registration, profile changes, document upload/replacement, verification, correction request, qualification, approval, rejection and suspension.
- RFQ lifecycle: creation, modification, publication, invitation, deadline changes, cancellation and closure.
- Quotation / bidding: draft submission, final submission, withdrawal, revised quotation version and submission timestamp.
- Evaluation: comparison changes, evaluation criteria, buyer remarks and supplier recommendation.
- Negotiation: round creation/closure, buyer request, supplier response, counteroffer and revised quotation version.
- Approval: task assignment, approve, reject, return for revision, delegation and comments.
- Award: award decision, selected supplier, unsuccessful supplier notifications and any reversal / cancellation.
- Notifications: queued, sent, failed, retried and delivery status where available.
- Integration: SAP mapping changes, outbound request, response, failure and reconciliation outcome.

## 3. Required audit event fields

Each event should record, where applicable:
- Unique event ID.
- Event timestamp in UTC.
- Actor user ID and effective role(s).
- Actor type: supplier, internal user, system process or integration.
- Action / event type.
- Target entity type and target record ID.
- Supplier organization / tenant scope.
- RFQ / quotation / approval correlation IDs where relevant.
- Outcome: success, failure or denied.
- Reason / business justification and user comments where required.
- Source IP and user-agent / session reference where permitted by privacy policy.
- Changed-field summary with before / after values for auditable business changes, avoiding secrets and unnecessary sensitive document contents.
- Correlation / request ID.

## 4. Integrity and access controls

- Audit records are append-only from normal application workflows; users cannot edit or delete historical events.
- No Supplier User, Buyer, Approver, Verifier, SRM Admin or Auditor may alter audit history.
- Super Admin access to audit data is read-only by default; privileged access attempts are themselves audited.
- Restrict audit visibility by role and organization scope. Auditor receives authorized read-only access; Super Admin can access platform-wide audit records for governance.
- Do not store passwords, access tokens, private keys or full sensitive document contents in logs.
- Use UTC timestamps and consistent event naming.
- Protect logs against unauthorized modification; production retention, backup and archival policy must be defined before deployment.
- Log access to sensitive audit exports and searches where appropriate.

## 5. Audit search and reports

Authorized users should be able to filter audit events by:
- Date/time range.
- Actor and role.
- Action / event type.
- Entity type and record ID.
- Supplier organization.
- RFQ, quotation or approval reference.
- Outcome (success, failure, denied).

Provide a read-only audit detail view and export capability for authorized audit roles, subject to access controls and export logging.

## 6. Implementation guidance

- Implement a centralized audit service and persist events in PostgreSQL.
- Write audit events transactionally with critical business state changes where feasible; use an outbox pattern for reliable asynchronous events when needed.
- Ensure failed and denied security-sensitive actions are captured without leaking secrets.
- Add automated tests for append-only behavior, role-based visibility, supplier data isolation and audit coverage of critical transitions.
- Define retention, archival, legal hold and export format policies with project stakeholders before production.

## 7. Non-functional requirements

- Audit logging must not expose secrets or create material delays in critical workflows.
- Audit events must be traceable and consistently correlated across RFQ, quotation, negotiation, approval and award.
- Audit data must be backed up and recoverable under the platform's agreed recovery objectives.
