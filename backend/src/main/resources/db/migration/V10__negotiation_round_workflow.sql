SET search_path TO srm, public;

ALTER TABLE negotiation_round
  ADD COLUMN response_deadline TIMESTAMPTZ,
  ADD COLUMN outcome VARCHAR(25),
  ADD COLUMN outcome_remarks TEXT,
  ADD COLUMN baseline_quotation_version_id UUID REFERENCES quotation_version(quotation_version_id);

ALTER TABLE negotiation_message
  ADD COLUMN quotation_version_id UUID REFERENCES quotation_version(quotation_version_id);

CREATE INDEX ix_negotiation_round_supplier_status
  ON negotiation_round (supplier_id, status, response_deadline);

CREATE INDEX ix_negotiation_message_round_created
  ON negotiation_message (negotiation_round_id, created_at);
