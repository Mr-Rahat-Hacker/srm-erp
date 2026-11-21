-- Supplier workflow status history is already modeled in V2.
-- This migration adds indexes for verifier queues and supplier review history.
CREATE INDEX IF NOT EXISTS idx_supplier_review_queue
    ON srm.supplier_organization (lifecycle_status, created_at);
CREATE INDEX IF NOT EXISTS idx_supplier_review_history
    ON srm.supplier_review (supplier_id, decided_at DESC);
