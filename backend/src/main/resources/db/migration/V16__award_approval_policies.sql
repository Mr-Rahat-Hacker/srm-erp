CREATE TABLE srm.award_approval_policy (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_code VARCHAR(50) NOT NULL UNIQUE,
    policy_name VARCHAR(160) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    minimum_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    maximum_amount NUMERIC(18,2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL REFERENCES srm.app_user(user_id),
    updated_by UUID NOT NULL REFERENCES srm.app_user(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_award_policy_amount_range
        CHECK (minimum_amount >= 0 AND (maximum_amount IS NULL OR maximum_amount > minimum_amount)),
    CONSTRAINT ck_award_policy_currency
        CHECK (currency_code ~ '^[A-Z]{3}$')
);

CREATE TABLE srm.award_approval_policy_stage (
    policy_id UUID NOT NULL REFERENCES srm.award_approval_policy(policy_id) ON DELETE CASCADE,
    sequence_number SMALLINT NOT NULL CHECK (sequence_number BETWEEN 1 AND 10),
    assigned_user_id UUID NOT NULL REFERENCES srm.app_user(user_id),
    PRIMARY KEY (policy_id, sequence_number),
    CONSTRAINT uq_award_policy_stage_approver UNIQUE (policy_id, assigned_user_id)
);

CREATE INDEX ix_award_approval_policy_match
    ON srm.award_approval_policy (currency_code, minimum_amount, maximum_amount)
    WHERE is_active = true;
