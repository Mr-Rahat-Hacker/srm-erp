-- Indexes used by server-side supplier and RFQ list filters, sorting, and counters.
CREATE INDEX IF NOT EXISTS ix_supplier_created_page
    ON srm.supplier_organization (created_at DESC, supplier_id);

CREATE INDEX IF NOT EXISTS ix_supplier_status_created_page
    ON srm.supplier_organization (lifecycle_status, created_at DESC, supplier_id);

CREATE INDEX IF NOT EXISTS ix_supplier_contact_primary_lookup
    ON srm.supplier_contact (supplier_id, contact_type);

CREATE INDEX IF NOT EXISTS ix_rfq_created_page
    ON srm.rfq (created_at DESC, rfq_id);

CREATE INDEX IF NOT EXISTS ix_rfq_buyer_created_page
    ON srm.rfq (buyer_user_id, created_at DESC, rfq_id);

CREATE INDEX IF NOT EXISTS ix_rfq_status_created_page
    ON srm.rfq (status, created_at DESC, rfq_id);

CREATE INDEX IF NOT EXISTS ix_rfq_line_rfq_count
    ON srm.rfq_line (rfq_id);

CREATE INDEX IF NOT EXISTS ix_rfq_invitation_rfq_count
    ON srm.rfq_invitation (rfq_id);
