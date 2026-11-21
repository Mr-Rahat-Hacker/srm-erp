# SRM State Machine Diagrams

**Status:** Design baseline for review  
**Version:** 1.0  
**Date:** 21 November 2026

These editable PlantUML state diagrams define the proposed lifecycle states and transitions for the first-release SRM scope.

## Diagram files

1. [Supplier lifecycle](state-supplier-lifecycle.puml)
2. [RFQ lifecycle](state-rfq-lifecycle.puml)
3. [Quotation lifecycle](state-quotation-lifecycle.puml)
4. [Approval task lifecycle](state-approval-workflow.puml)

## Modeling rules

- A state represents the current lifecycle condition of a business object.
- A transition must be caused by a user action, system event, policy outcome or authorized integration event.
- Rejection, hold, send-back, suspension, cancellation and disqualification require a reason and an audit event.
- Submitted quotation versions are immutable. Revisions create a new version and retain history.
- Supplier organizations must not access competing suppliers' quotations or negotiation data.
- Approval actions are restricted to assigned approvers or authorized delegates. Administrative access alone does not grant approval authority.
- SAP Vendor Master remains owned by SAP. SRM maps an existing SAP Vendor Code; it does not create Vendor Master records.

## Decisions still requiring business confirmation

1. Must an SAP Vendor Code be mapped before a supplier becomes eligible for RFQs, or can activation occur before mapping?
2. Which supplier review stages are mandatory for every supplier, and which depend on supplier category or material group?
3. Can an RFQ be published without approval for selected categories or value bands?
4. What event closes the quotation submission window: deadline reached, buyer closes early, or both under authorization?
5. Which RFQ evaluation paths require technical evaluation before commercial evaluation?
6. What are the rules for minimum quotation count, deadline extension and proceeding by exception?
7. Which approval stages and value/deviation thresholds apply to rate recommendations and awards?
8. Does a sent-back approval task reactivate the same task or create a new task linked to the previous one? The implementation must preserve the complete history either way.

## Important implementation note

These diagrams are a proposed lifecycle model, not a claim that every policy decision is finalized. Resolve the open decisions before implementing state-transition services and database constraints. PlantUML source is committed; PNG rendering and automated syntax validation have not yet been performed.
