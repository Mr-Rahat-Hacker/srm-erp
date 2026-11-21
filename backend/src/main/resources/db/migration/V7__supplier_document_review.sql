ALTER TABLE srm.supplier_document
  ADD COLUMN reviewed_by UUID REFERENCES srm.app_user(user_id),
  ADD COLUMN reviewed_at TIMESTAMPTZ,
  ADD COLUMN review_remarks TEXT;

ALTER TABLE srm.supplier_document
  ADD CONSTRAINT ck_supplier_document_review_status
  CHECK (review_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED'));

CREATE INDEX idx_supplier_document_expiry
  ON srm.supplier_document(expiry_date)
  WHERE expiry_date IS NOT NULL;
