-- liquibase formatted sql

-- changeset developer:5
-- Remove all feature permissions for role_id 2 (user/customer) as they do not have dashboard access

DELETE FROM tm_role_features WHERE role_id = 2;
