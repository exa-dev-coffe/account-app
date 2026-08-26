-- liquibase formatted sql

-- changeset developer:3
-- Sync identity sequences for tables with manual seed data

SELECT setval(pg_get_serial_sequence('tm_roles', 'role_id'), COALESCE((SELECT MAX(role_id) FROM tm_roles), 1));
SELECT setval(pg_get_serial_sequence('tm_accounts', 'user_id'), COALESCE((SELECT MAX(user_id) FROM tm_accounts), 1));
SELECT setval(pg_get_serial_sequence('tm_features', 'feature_id'), COALESCE((SELECT MAX(feature_id) FROM tm_features), 1));
SELECT setval(pg_get_serial_sequence('tm_role_features', 'id'), COALESCE((SELECT MAX(id) FROM tm_role_features), 1));
