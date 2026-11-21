# SRM ERP — MVP Scope

## 1. Product objective

Build an independent Supplier Relationship Management and procurement ERP application for manufacturing operations. The system should support the procurement lifecycle and later exchange approved data with SAP.

## 2. MVP modules

### 2.1 Master data
- Supplier master: identity, addresses, contacts, tax and payment details, status.
- Material master: material code, description, type, base unit, group and status.
- Purchasing data: supplier-material relationship, lead time, currency, pricing validity and minimum order quantity.
- Plant and storage-location reference data.

### 2.2 Sourcing
- Request for Quotation (RFQ) creation and supplier invitation.
- Supplier quotation capture.
- Commercial and delivery comparison.
- Quotation selection with approval history.

### 2.3 Purchasing
- Purchase Requisition (PR) creation and approval.
- Purchase Order (PO) creation from approved requisition or sourcing result.
- PO revision, release status and delivery tracking.
- Schedule Agreement creation and delivery schedule maintenance.
- Purchase Info Record maintenance linking supplier and material.

### 2.4 Receiving and invoice tracking
- Goods Receipt Note (GRN) against PO.
- Received quantity and discrepancy tracking.
- Supplier invoice reference and invoice status tracking.
- Three-way matching foundation: PO, GRN and invoice.

### 2.5 Reporting and audit
- Open PR and PO reports.
- Supplier delivery performance.
- Spend and quotation comparison reports.
- Audit trail for important master and transaction changes.

## 3. Key business rules

- Supplier and material codes must be unique within their defined scope.
- A PO must reference an active supplier and valid material.
- PO quantity and price must be positive.
- Goods receipt cannot exceed the permitted PO balance unless an authorized tolerance is configured.
- A transaction must retain its creator, timestamps and status history.
- Approved records must not be silently overwritten; changes must be versioned or audited.

## 4. Roles (initial)

- Administrator
- Procurement User
- Procurement Approver
- Stores / Goods Receipt User
- Finance / Invoice User
- Read-only Auditor

Role permissions will be refined before authentication implementation.

## 5. Out of scope for the first MVP

- Full accounting and general ledger.
- Payroll and HR.
- Direct SAP database access.
- Uncontrolled automatic posting into SAP.
- Mobile-native application.

## 6. Delivery sequence

1. Architecture, requirements and data model.
2. Backend and database foundation.
3. Authentication and role-based access.
4. Supplier and material master.
5. Purchase Info Record and Schedule Agreement.
6. RFQ and quotation comparison.
7. PR, approval and PO.
8. GRN and invoice tracking.
9. SAP integration adapter and reporting.
