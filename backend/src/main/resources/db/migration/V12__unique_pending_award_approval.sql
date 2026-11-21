-- An RFQ recommendation can have only one unresolved approval request at a time.
CREATE UNIQUE INDEX uq_pending_award_approval_per_recommendation
    ON srm.approval_request (entity_id)
    WHERE entity_type = 'AWARD' AND status = 'PENDING';
