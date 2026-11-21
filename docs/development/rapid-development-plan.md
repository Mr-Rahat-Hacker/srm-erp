# SRM Rapid Development Plan

**Basis:** README, MVP Scope, SRS 1.0, SRM Workflow, RBAC Permission Matrix and Detailed Business Requirements Review.

## Delivery approach

Ship one narrow, end-to-end vertical slice at a time. Each slice includes its database/API contract, role checks, Angular workflow, audit events, automated tests and a GitHub Actions result before it is merged. Keep implementation on short-lived branches and do not mark a feature complete from a frontend build alone.

## Requirements that govern sequencing

- Supplier onboarding and qualification precede RFQ invitation.
- Supplier, document, quotation and approval access must be scoped to the authenticated organization or assignment.
- Preserve status, document and quotation history; do not silently overwrite submitted records.
- SAP remains the owner of Vendor Master and Material Master. SRM may map an existing SAP Vendor Code only after the authorized SAP process creates it.
- PIR and purchasing-condition writes are the final initial-release integration milestone, through authorized SAP APIs or middleware only.
- Do not hard-code the example monetary approval thresholds in the reference review.
- Resolve the documented policy questions before making SAP mapping a supplier activation gate or hard-coding SCM/Finance/Quality review stages.

## Prioritized vertical slices

### 0. Stabilize supplier workflow and test feedback — active

- Correct the supplier submission audit actor to match the SRM_ADMIN-only endpoint.
- Add focused tests for permitted lifecycle transitions, rejected transitions, required decision remarks and missing suppliers.
- Keep document upload restricted to internal authorized roles until supplier membership and ownership enforcement exist.
- Extend backend and frontend tests as each slice adds business behavior; run both suites in GitHub Actions.

### 1. Supplier qualification and identity

- Build the verifier queue and supplier lifecycle actions: start review, request correction, resubmit, approve and reject.
- Show lifecycle and document history in the profile and capture decision reasons.
- Implement authenticated supplier membership and enforce ownership on profile and document APIs before enabling supplier-side changes.
- Add controlled mapping of an existing SAP Vendor Code with audit history; never create SAP vendors in SRM.
- Confirm whether SAP mapping is required for RFQ eligibility and whether qualification stages vary by supplier category.

### 2. RFQ creation and invitations

- Create RFQs with SAP Material Code references, lines, quantities, UOM, deadline, terms and draft/publish lifecycle.
- Allow buyers to invite only eligible suppliers; show suppliers only their own invitations.
- Test deadline validation, duplicate invitations, eligibility and cross-supplier denial.

### 3. Quotation submission and revision history

- Let invited suppliers create and submit quotations against RFQ lines.
- Validate prices, currency, validity and deadlines; retain each submitted version.
- Test ownership, confidentiality between suppliers, deadline behavior and no silent overwrite.

### 4. Evaluation and negotiation

- Add line-by-line technical/commercial evaluation, recorded remarks and versioned negotiation rounds.
- Keep previous evaluations, messages and quotation versions available to authorized users.
- Test role boundaries, response deadlines and revision comparisons.

### 5. Recommendation, approval and award

- Record buyer recommendation and justification, then route through configurable assigned approval tasks.
- Enforce assignment and segregation-of-duties rules; capture approve, reject, return and hold outcomes with reasons.
- Record award separately from approval and make the approved outcome read-only through normal workflows.

### 6. Notifications, dashboards and audit search

- Add in-app/email status notifications with retry visibility.
- Add supplier, RFQ, quotation and award dashboards and scoped audit search.
- Test notification failure/retry, read-only auditor behavior and audit access logging.

### 7. Controlled SAP PIR integration

- Start only after the preceding SRM workflow and SAP API authorization are confirmed.
- Use an approved SAP API/integration layer, validation, approval, idempotency, sanitized errors and reconciliation.
- Prohibit direct SAP database writes.

## Definition of done for each slice

1. Acceptance cases map to SRS requirements and include allowed and denied role/ownership cases.
2. Database changes use forward-only, versioned migrations.
3. Business state changes and audit records are transactional where feasible.
4. Backend unit/integration tests and Angular tests pass in GitHub Actions.
5. API and user-visible error messages are clear; secrets and document contents do not enter logs.
6. The pull request includes scope, test evidence and any unresolved business decision; merge only after checks pass.
