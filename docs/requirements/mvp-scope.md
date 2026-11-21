# SRM Platform — First Release Scope

## 1. Product objective

Build an independent cloud-based Supplier Relationship Management application that reduces manual supplier-management and procurement coordination work. The first release focuses on supplier-facing and internal SRM workflows.

The platform complements SAP in complex manufacturing environments; it does not replace SAP for master-data ownership or procurement execution.

## 2. First-release functional scope

### 2.1 Supplier registration and profile
- Supplier self-registration.
- Company profile and address information.
- Supplier contact management.
- Document upload for GST, PAN, certifications and other configured documents.

### 2.2 Supplier verification and qualification
- Internal document and profile verification.
- Request correction / resubmission.
- Approve or reject supplier registration.
- Supplier qualification status and history.
- Map an existing SAP Vendor Code after the authorized SAP process creates the vendor master.

### 2.3 RFQ management
- Buyer creates RFQ with RFQ number, title, scope, required-by deadline and terms.
- RFQ line items reference SAP Material Codes, descriptions, quantities, UOM and plant/reference information.
- SRM does not create or maintain SAP Material Master.
- Invite multiple qualified suppliers.
- Supplier portal access to invited RFQs.
- RFQ status, deadline and communication tracking.

### 2.4 Supplier quotation / bidding
- Invited suppliers submit quotations against RFQ line items.
- Capture price, currency, taxes/charges where configured, delivery lead time, validity and commercial remarks.
- Validate required fields and submission deadline.
- Record submission time and supplier identity.
- Prevent silent overwrite of submitted quotations.
- Allow authorized withdrawal or resubmission only according to RFQ rules, preserving history.

### 2.5 Quotation comparison and commercial evaluation
- Compare supplier quotations line by line.
- Compare price, total commercial value, delivery lead time, validity and other configured commercial criteria.
- Highlight missing or non-comparable information.
- Record buyer evaluation remarks and recommendation.
- Preserve evaluation history.

### 2.6 Negotiation
- Buyer initiates negotiation with one or more invited / participating suppliers according to policy.
- Record negotiation round, message, requested change and response deadline.
- Supplier submits revised quotation as a new version.
- Preserve all previous quotation versions and negotiation communication.
- Show version-to-version commercial changes.
- Close negotiation with a recorded outcome.

### 2.7 Recommendation, approval and award
- Buyer records supplier recommendation and commercial justification.
- Configurable approval workflow with role-based access.
- Approvers review comparison, negotiation history and justification inside the authenticated application.
- Record approve / reject / return-for-revision decisions with user, time and comments.
- Record final award decision.
- Notify selected and unsuccessful suppliers.

### 2.8 Communication, audit and reporting
- Email and in-application notifications.
- RFQ and supplier status tracking.
- Audit trail for important actions and decisions.
- Dashboard and reports for supplier onboarding, active RFQs, quotations, negotiation and awards.

## 3. Explicit exclusions from the first release

The following are NOT first-release SRM modules:
- Material Master creation or maintenance.
- SAP Vendor Master / Business Partner creation.
- Purchase Requisition (PR) creation and approval.
- Purchase Order (PO) creation or execution.
- Goods Receipt Note (GRN) / goods receipt.
- Invoice verification.
- MRP.
- Schedule Agreement execution / delivery scheduling.

These remain in SAP for the initial manufacturing use case unless separately approved in a future product scope.

## 4. SAP boundary

- SAP is system of record for SAP Vendor Master / Business Partner and Material Master.
- SRM maintains its own internal Supplier ID and maps an existing SAP Vendor Code after authorized SAP creation.
- RFQ line items refer to SAP Material Codes; SRM does not create SAP materials.
- Final initial-SRM integration milestone: controlled SAP Purchase Info Record (PIR) and relevant purchasing rate/condition creation or update.
- SAP writes must use authorized APIs / integration, validations, approvals, logs and traceability.
- No direct database writes to SAP.
- Approvals occur inside the authenticated application, not through unauthenticated email links.

## 5. Main user roles

- Supplier User
- Procurement / Buyer User
- Supplier Verification / Qualification User
- Procurement Approver
- SRM Administrator
- Auditor / Read-only User

Detailed permissions will be specified in the role matrix.

## 6. Core workflow

Supplier Registration → Document Verification → Correction / Qualification → Approved Supplier → RFQ Creation → Supplier Invitation → Quotation Submission → Commercial Comparison → Negotiation Rounds → Revised Quotation Versions → Recommendation → Approval → Award → Supplier Notifications → Audit / Reporting.

## 7. Future expansion (not first-release commitment)

Broader cloud procurement may later include purchase requisition, PO request and SAP hand-off, contract lifecycle, delivery collaboration, supplier scorecards, spend analytics, budget controls and additional ERP integrations. Prioritize these only after the SRM release is proven.
