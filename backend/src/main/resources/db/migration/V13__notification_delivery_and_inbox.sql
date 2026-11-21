ALTER TABLE srm.notification
    ADD COLUMN title VARCHAR(180),
    ADD COLUMN body TEXT,
    ADD COLUMN target_path VARCHAR(500),
    ADD COLUMN read_at TIMESTAMPTZ,
    ADD COLUMN last_error TEXT;

CREATE INDEX ix_notification_recipient_inbox
    ON srm.notification (recipient_user_id, created_at DESC);

CREATE INDEX ix_notification_delivery_queue
    ON srm.notification (created_at)
    WHERE channel = 'EMAIL' AND delivery_status = 'QUEUED';
