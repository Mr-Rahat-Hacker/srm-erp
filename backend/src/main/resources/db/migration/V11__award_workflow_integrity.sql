-- Enforce a single final award per RFQ even when concurrent approval requests exist.
CREATE UNIQUE INDEX uq_award_one_final_per_rfq
    ON srm.award (rfq_id)
    WHERE award_status = 'APPROVED';

CREATE INDEX ix_approval_task_assignee_status
    ON srm.approval_task (assigned_user_id, task_status, approval_request_id);

CREATE INDEX ix_approval_request_entity_status
    ON srm.approval_request (entity_type, status, entity_id);
