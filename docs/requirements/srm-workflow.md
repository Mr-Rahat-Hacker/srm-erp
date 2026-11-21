# SRM Core Workflow — First Release

## A. Supplier onboarding
1. Supplier self-registers and submits company profile and contacts.
2. Supplier uploads required documents.
3. Internal verifier reviews profile and documents.
4. Verifier requests corrections, approves qualification or rejects with reasons.
5. Approved supplier becomes eligible for RFQ invitations.
6. Existing SAP Vendor Code is mapped only after authorized SAP Vendor Master creation.

## B. RFQ to award
1. Buyer creates RFQ and adds lines using SAP Material Code references, descriptions, quantities, UOM and plant/reference data.
2. Buyer selects approved suppliers and issues invitations.
3. Invited suppliers review RFQ and submit quotations before deadline.
4. System validates and timestamps each submission; submitted versions remain traceable.
5. Buyer compares quotations commercially and records evaluation.
6. Buyer opens negotiation rounds and records requests, supplier responses and deadlines.
7. Supplier submits revised quotation; each revision becomes a new version and prior versions remain available.
8. Buyer records supplier recommendation and commercial justification.
9. Authorized approver reviews the complete RFQ, comparison and negotiation history in the authenticated application.
10. Approver approves, rejects or returns for revision with comments.
11. Buyer records award decision and system notifies selected and unsuccessful suppliers.
12. System retains audit trail, status history and communication records.

## C. Core lifecycle states (initial proposal)

Supplier: Draft → Submitted → Under Review → Correction Required → Resubmitted → Qualified / Rejected → Active / Suspended.

RFQ: Draft → Approval Pending (if configured) → Published / Invitations Sent → Quotation Open → Evaluation → Negotiation → Approval Pending → Awarded / Closed / Cancelled.

Quotation: Draft → Submitted → Under Evaluation → Negotiation Requested → Revised (versioned) → Accepted / Not Selected / Withdrawn.

Approval: Pending → Approved / Rejected / Returned for Revision.

These state names and transition permissions must be validated in SRS and the activity diagrams before implementation.

## D. First-release exclusions

No Material Master creation, Vendor Master creation, PR, PO, GRN, invoice verification, MRP or Schedule Agreement execution.

## E. Final initial-SRM SAP integration milestone

After approval and award, support controlled SAP PIR and relevant purchasing rate/condition creation or update through authorized SAP APIs / integration, with validations, authorization, logs and traceability.
