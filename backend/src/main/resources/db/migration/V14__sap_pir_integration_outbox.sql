ALTER TABLE srm.sap_integration_event
    ADD COLUMN request_key UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN request_payload JSONB,
    ADD COLUMN response_payload JSONB,
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX uq_sap_integration_request_key
    ON srm.sap_integration_event (request_key);

CREATE UNIQUE INDEX uq_sap_pir_request_per_award
    ON srm.sap_integration_event (award_id, operation_type)
    WHERE award_id IS NOT NULL;

CREATE INDEX ix_sap_integration_queue
    ON srm.sap_integration_event (created_at)
    WHERE status = 'QUEUED';
