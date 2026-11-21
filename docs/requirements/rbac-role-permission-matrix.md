# SRM Platform — RBAC and Role Permission Matrix

## 1. Purpose

Role-Based Access Control (RBAC) governs what each authenticated user can view and do in the SRM platform. Authorization is enforced by the Spring Boot backend; hiding a button in Angular is not a security control.

Use least privilege, deny by default, and validate both role permissions and record ownership / assignment.

## 2. User roles

### External role
- **SUPPLIER_USER** — supplier organization user who manages its own profile, documents, invited RFQs, quotations and negotiation responses.

### Internal roles
- **SUPPLIER_VERIFIER** — reviews supplier registrations and documents, requests corrections and records verification outcomes.
- **PROCUREMENT_BUYER** — manages qualified supplier selection for RFQs, creates RFQs, invites suppliers, evaluates bids, negotiates and recommends award.
- **PROCUREMENT_APPROVER** — reviews assigned approval requests and approves, rejects or returns them for revision.
- **SRM_ADMIN** — manages day-to-day application configuration and operational user support within permissions delegated by the Super Admin. Does not automatically grant authority to approve a business transaction.
- **SUPER_ADMIN** — highest platform governance role. Manages SRM administrators, global security settings, role/permission definitions, tenant / organization configuration and emergency access. Super Admin actions are always audited. Super Admin does not silently alter or approve business decisions; exceptional access must be justified and traceable.
- **AUDITOR** — read-only access to authorized business records, approval decisions and audit history.

A person may hold more than one role only where organizational policy permits it. Conflicting duties must be controlled. SUPER_ADMIN is a separately controlled, highly privileged role and should be assigned to very few trusted platform owners.

## 3. Permission matrix

Legend: C = create, R = read, U = update, A = action / decision, — = no access by default.

| Capability | Supplier User | Supplier Verifier | Procurement Buyer | Approver | SRM Admin | Auditor |
|---|---:|---:|---:|---:|---:|---:|
| Own supplier profile | C/R/U | R | R (qualified suppliers) | R (assigned context) | Admin support | R |
| Own supplier documents | C/R/U | R/A (verify) | R (if qualified / invited context) | R (assigned context) | Admin support | R |
| Verify / qualify supplier | — | A | R | R (assigned context) | Configure roles only | R |
| Create / manage RFQ | — | — | C/R/U | R (assigned context) | Configure only | R |
| Invite qualified suppliers | — | — | C/R/U | R (assigned context) | Configure only | R |
| View invited RFQ | R (own invitation) | — | R/U | R (assigned context) | Support access, audited | R |
| Submit / revise own quotation | C/R/U (own draft; submit version) | — | R | R (assigned context) | No business edit | R |
| Compare quotations | — | — | R/C/U | R (assigned context) | No business edit | R |
| Run negotiation | R/A (own response) | — | C/R/U | R (assigned context) | No business edit | R |
| Recommend supplier | — | — | A | R | No business edit | R |
| Approve / reject / return | — | — | — | A (assigned tasks only) | — | R |
| Record award | — | — | A (after approval) | A only if explicitly assigned by workflow | — | R |
| View audit history | Own activity only | Authorized scope | Authorized scope | Assigned scope | R (authorized scope) | R (global) | R |
| Manage own supplier users (if enabled) | A (own organization only) | — | — | — | Support / delegated | — | — |
| Manage internal users / roles | — | — | — | — | A (delegated) | A (global) | — |
| Manage global security / platform settings | — | — | — | — | — | A | — |
| Configure workflow / reference settings | — | — | — | — | A | R |

The matrix is an initial product policy. Exact field-level access and organization-specific delegation must be confirmed during detailed design.

## 4. Mandatory authorization rules

1. Deny access by default. Every protected backend endpoint requires authentication and an explicit permission.
2. Enforce authorization server-side using Spring Security. Angular route guards and hidden UI controls are supplementary only.
3. Enforce supplier data isolation: a supplier user may access only its own organization profile, documents, RFQ invitations, quotations and negotiation messages.
4. Enforce assignment scope: approvers may act only on approval tasks assigned to them or to an authorized group.
5. A supplier may never view another supplier's quotation, bid, commercial response or negotiation messages.
6. A buyer may access quotations only for RFQs within the buyer's authorized scope.
7. Supplier verification decisions must be made by an authorized verifier; a supplier cannot verify itself.
8. Approval decisions must be made by an authorized approver inside the authenticated application. Email links may notify but must not approve.
9. Apply segregation of duties: by default, the user who submitted a recommendation cannot approve that same award. Any exception must be explicitly configured and audited.
10. SRM_ADMIN manages identities and permissions but does not receive automatic business approval powers.
11. AUDITOR is read-only; no create, update, delete, submit, negotiate, approve or award actions.
12. SUPER_ADMIN privileges are limited to platform governance and emergency administration; every privileged action must capture actor, timestamp, target, action, reason and outcome. Super Admin must not edit quotation values, negotiation records, approval decisions or award outcomes as an ordinary administrative shortcut.
13. Record successful and denied sensitive authorization events where appropriate, and always audit business decisions and role/permission changes.
14. Revoke or re-evaluate access when a user is disabled, supplier is suspended, invitation expires or assignment changes.
15. Never trust a supplier ID, RFQ ID, quotation ID or approval task ID supplied by the client without checking the authenticated user's relationship to that record.

## 5. Authentication and access lifecycle

- Authenticate users before protected operations.
- Use secure password hashing and JWT-based authentication with expiry and revocation / refresh strategy defined in security design.
- Associate each supplier user with a supplier organization.
- Associate internal users with one or more approved roles and, where needed, business scope such as purchasing organization / plant / team.
- Keep role assignment changes auditable.
- Apply account activation, suspension and deactivation controls.

## 6. Implementation direction

- Spring Security for authentication and authorization.
- Role / authority mapping in the backend.
- Method-level authorization for sensitive service operations.
- Resource-level checks for supplier ownership, RFQ invitation and approval assignment.
- Angular route guards and permission-aware navigation for user experience only.
- Automated tests for allowed and denied access, including cross-supplier data access attempts.

## 7. Open decisions for SRS

- Can one supplier organization have multiple supplier users and an organization administrator?
- Are buyer permissions global or scoped by purchasing organization / plant / category?
- Can approvers approve only individually assigned tasks or tasks assigned to their approval group?
- What delegation and out-of-office approval rules are required?
- What is the required multi-factor authentication policy?
