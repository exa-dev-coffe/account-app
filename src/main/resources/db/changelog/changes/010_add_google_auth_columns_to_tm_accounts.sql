-- liquibase formatted sql

-- changeset developer:10 validCheckSum:ANY
-- Add google_sub and google_email columns to tm_accounts for Google Sign In / Binding support

ALTER TABLE tm_accounts
    ADD COLUMN google_sub VARCHAR(255) DEFAULT NULL,
    ADD COLUMN google_email VARCHAR(255) DEFAULT NULL;

CREATE INDEX idx_tm_accounts_google_sub
    ON tm_accounts (google_sub)
    WHERE deleted_at IS NULL;

-- rollback DROP INDEX IF EXISTS idx_tm_accounts_google_sub;
-- rollback ALTER TABLE tm_accounts DROP COLUMN IF EXISTS google_email;
-- rollback ALTER TABLE tm_accounts DROP COLUMN IF EXISTS google_sub;

