-- Accelerate the contains-style ILIKE searches used by supplier and RFQ list filters.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS ix_supplier_legal_name_trgm
    ON srm.supplier_organization USING gin (legal_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_supplier_trade_name_trgm
    ON srm.supplier_organization USING gin ((COALESCE(trade_name, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_supplier_number_trgm
    ON srm.supplier_organization USING gin (supplier_number gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_supplier_contact_email_trgm
    ON srm.supplier_contact USING gin ((COALESCE(email, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_rfq_number_trgm
    ON srm.rfq USING gin (rfq_number gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_rfq_title_trgm
    ON srm.rfq USING gin (title gin_trgm_ops);
