-- IAM baseline: prevent duplicate case-insensitive login identities.
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email_lower ON srm.app_user (lower(email));

-- Passwords are stored as BCrypt hashes in app_user.password_hash.
-- User creation and role assignment are intentionally provisioned through a trusted admin process,
-- not through a public endpoint or a hardcoded bootstrap credential.
