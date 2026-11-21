# Detailed Business Requirements Review — Uploaded Reference

**Source:** User-provided `Pasted text.txt`  
**Review date:** 21 November 2026  
**Status:** Reviewed; proposed additions and scope conflicts require baseline approval

## 1. Purpose

This document records how the uploaded manufacturing / automotive SRM business scenario relates to the existing SRM Product Vision and SRS. It is a requirements review, not an authorization to expand the first-release product into a full procurement ERP.

## 2. Requirements aligned with the existing SRM baseline

The uploaded scenario reinforces these existing Phase-1 capabilities:

- Supplier self-registration, profile and document submission.
- Internal supplier verification, correction requests, qualification and rejection.
- RFQ creation with SAP Material Code references.
- Invitations to multiple qualified suppliers.
- Supplier quotation submission and version history.
- Commercial comparison and negotiation.
- Supplier recommendation, approval, award and notifications.
- Role-based dashboards, status tracking, reporting and audit trail.
- Future authorized SAP integration for purchasing information.

## 3. Proposed Phase-1 requirement refinements

The following details are useful additions to the existing SRS and should be considered in the next SRS revision:

### 3.1 Supplier onboarding and approval

- Capture registered and communication addresses, GST, PAN, MSME details, supplier/product categories, manufacturing capabilities, certifications, contacts and configured financial details.
- Support configurable document types, including GST certificate, PAN, MSME certificate, cancelled cheque / bank confirmation, ISO and quality certificates.
- Support staged review by SCM, Finance and Quality, followed by an authorized final approver where company policy requires it.
- Support approve, reject, send back for correction, hold and request additional information.
- Capture reviewer, role, timestamp, remarks, old/new status and supporting attachments.
- Permit suppliers to edit only permitted sections after a correction request.

Sensitive bank and tax information requires field-level authorization, secure storage and data minimization. Exact retention and masking rules remain to be decided.

### 3.2 Supplier activation and SAP vendor mapping

The existing product baseline states that SAP owns Vendor Master / Business Partner and SRM maps an existing SAP Vendor Code after the authorized SAP process creates it.

The uploaded scenario says supplier activation may occur through an internal action or after future SAP vendor creation. These statements need a clear lifecycle decision:

- Keep SRM qualification/approval distinct from SAP vendor creation.
- Define whether an SRM-approved supplier may become RFQ-eligible before SAP Vendor Code mapping.
- If SAP Vendor Code is mandatory for activation in a particular deployment, make that a configurable business gate, not an SRM vendor-master creation feature.
- Do not create SAP Vendor Master / Business Partner records in this Phase-1 scope.

### 3.3 RFQ approval and publishing

- Add configurable RFQ approval before publishing.
- Validate at least one RFQ line, at least one eligible supplier, valid deadline, required commercial fields and required attachments.
- Detect or warn about duplicate active RFQs.
- Track invitation, acknowledgement, participation and decline; require a decline reason.
- Support authorized deadline extension and notify all invited suppliers.
- Support reminders and a controlled decision when supplier response count is below the configured minimum.

### 3.4 Quotation and bid confidentiality

- Capture item-level basic price, discount, net price, taxes, freight, packing/forwarding, tooling/development, insurance, other charges and total landed cost.
- Capture MOQ, lead time, delivery schedule, payment terms, Incoterms, validity, warranty, origin, technical/commercial deviations, remarks and attachments as configured.
- Calculate net price and landed cost using a documented, configurable pricing formula.
- Keep supplier bids confidential: suppliers must not see competitors' identity, participation status, pricing or attachments.
- Lock quotation submission after deadline unless an authorized extension or exception is recorded.
- Retain every submitted revision as a separate version.

### 3.5 Technical and commercial evaluation

- Support technical evaluation before commercial evaluation when the configured bid process requires it.
- Technical outcomes: Technically Accepted, Technically Rejected, Clarification Required and Conditionally Accepted.
- Permit commercial evaluation only for technically accepted suppliers when the technical-commercial bid policy is enabled.
- Comparison should support line-level and supplier-level commercial fields, landed cost, delivery, payment terms, validity, technical status, quality/performance ratings and authorized benchmark / previous purchase price references.
- Default L1/L2/L3 ranking should use total landed cost. The comparison basis must be configurable and visible to users.

### 3.6 Negotiation and settled rate

- Support multiple negotiation rounds with round number, supplier, previous/revised price, revised terms, date, actor, supplier confirmation, remarks, attachments and version history.
- Calculate savings amount and percentage against the defined baseline.
- Preserve original quotation and all negotiation values permanently.
- Record final settled rate and calculate original value, final value, savings, benchmark variance, previous purchase price variance and award value.

### 3.7 Recommendation and rate approval

- Support recommendation of a non-L1 supplier only with mandatory justification.
- Non-L1 recommendation must trigger an additional configured approval / exception route.
- Approval routing may consider total business value, plant, purchasing group, material category, project, price deviation and L1/non-L1 status.
- Monetary thresholds and approval levels are configuration data, not hard-coded rules.
- Approvers need access to RFQ, quotations, technical result, comparison, negotiation history, settled rate, savings, benchmark context, justification, deviations and approval history.
- Support approve, reject, send back, hold, clarification and authorized delegation.
- Require remarks for rejection, send-back, hold and configured exception/deviation approvals.
- Resubmission must preserve prior data and approval history.

The sample ₹5 lakh / ₹25 lakh thresholds in the uploaded scenario are examples only. They are not approved business rules.

### 3.8 Award and closure

- Award may be issued only after required rate approvals.
- Selected supplier receives award communication; unsuccessful suppliers receive closure communication without competitor-confidential information.
- Awarded supplier and approved settled rate become read-only after award.
- Changes after award require a formal amendment workflow.
- Track Award Approved, Awarded and Closed as distinct milestones where business policy requires them.

## 4. Role mapping

The uploaded reference has Super Admin, Admin, Buyer, Approver, Supplier and Auditor.

Recommended mapping to the existing seven-role RBAC baseline:

| Uploaded role / subtype | Existing SRM role mapping |
|---|---|
| Super Admin | `SUPER_ADMIN` |
| Admin | `SRM_ADMIN` |
| Internal company user / Buyer | `PROCUREMENT_BUYER` |
| SCM / Finance / Quality / Technical / Costing reviewer and managers | Approval role / authority attributes assigned to `PROCUREMENT_APPROVER`, or verifier authority where appropriate |
| Supplier | `SUPPLIER_USER` |
| Auditor / Read-only | `AUDITOR` |

Do not automatically create a separate application role for every approver title. Use configurable approval groups, authority scopes and workflow assignments unless stakeholders require distinct permissions.

The existing `SUPPLIER_VERIFIER` role remains for supplier verification and qualification. Approval authority for final supplier approval must be explicitly assigned.

## 5. Scope conflicts and decisions

| Topic | Existing baseline | Uploaded reference | Treatment |
|---|---|---|---|
| SAP Vendor Master | SAP owns it; SRM maps existing code | Activation may follow future SAP creation | Preserve SAP ownership; decide RFQ eligibility gate |
| SAP Material Master | SAP owns it; RFQ uses material references | RFQ captures material/part codes and technical details | Compatible; no SRM material creation |
| RFQ approval | Not fully detailed in first workflow baseline | Configurable approval before publish | Proposed Phase-1 addition |
| Technical evaluation | Not explicitly detailed in initial workflow | May precede commercial evaluation | Proposed Phase-1 addition |
| Rate approval | Configurable approval after recommendation | Multi-factor / value-based approval | Refine SRS; no hard-coded thresholds |
| PO / contracts / scheduling agreements | Excluded from Phase-1 | Mentioned as future phases | Keep future scope only |
| Supplier bank details | Document/profile data is in baseline | Detailed bank information and verification | Include only with explicit access, privacy and retention controls |

## 6. Explicitly future scope

The uploaded reference's future statements about SAP contracts, Purchase Orders, Scheduling Agreements and Outline Agreements remain outside the first-release SRM build. They may be assessed in a later product roadmap.

## 7. Source limitation

The uploaded text ends during the Approver Dashboard section. Dashboard requirements after that point were not available and have not been inferred.

## 8. Recommended next decisions

Before final SRS approval, confirm:
1. Whether SAP Vendor Code mapping is mandatory before a supplier becomes RFQ-eligible.
2. Whether supplier approval must always include SCM, Finance, Quality and final approver, or whether stages are configurable by supplier category.
3. Whether RFQ approval is mandatory for every RFQ or configurable by plant / value / category.
4. Which bid confidentiality / sealed-bid rules apply and who can unlock commercial bids.
5. The exact landed-cost formula and tax/freight treatment.
6. Which supplier ratings and historical prices may be used in comparison, and who may view them.
7. Approval routing dimensions, delegation and exception rules.
8. Post-award amendment policy.

