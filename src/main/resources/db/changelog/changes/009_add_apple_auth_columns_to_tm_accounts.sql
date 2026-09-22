-- liquibase formatted sql

-- changeset developer:9
-- Add apple_sub and apple_email columns to tm_accounts for Apple Sign In support

ALTER TABLE tm_accounts
    ADD COLUMN apple_sub VARCHAR(255) DEFAULT NULL,
    ADD COLUMN apple_email VARCHAR(255) DEFAULT NULL;

CREATE INDEX idx_tm_accounts_apple_sub
    ON tm_accounts (apple_sub)
    WHERE deleted_at IS NULL;
