-- liquibase formatted sql

-- changeset developer:6
-- Add soft delete columns to tm_accounts and replace global unique constraint on email with partial unique index (ignoring soft-deleted accounts)

ALTER TABLE tm_accounts
    ADD COLUMN deleted_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    ADD COLUMN deleted_by INTEGER DEFAULT NULL;

ALTER TABLE tm_accounts
    DROP CONSTRAINT IF EXISTS uc_tm_accounts_email;

CREATE UNIQUE INDEX tm_accounts_email_unique_idx
    ON tm_accounts (email)
    WHERE deleted_at IS NULL;
