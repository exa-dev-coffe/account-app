-- liquibase formatted sql

-- changeset developer:8
-- Add wallet_management feature and assign full permissions to default admin (role_id 1)

INSERT INTO tm_features (feature_id, feature_key, feature_name, description)
VALUES (13, 'wallet_management', 'Wallet Management', 'Manage customer digital wallets, view transaction histories, and trigger/reset PIN via email verification')
ON CONFLICT (feature_id) DO NOTHING;

-- Grant to Admin (role 1)
INSERT INTO tm_role_features (role_id, feature_id, can_view, can_create, can_edit, can_delete)
VALUES (1, 13, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (role_id, feature_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('tm_features', 'feature_id'), COALESCE((SELECT MAX(feature_id) FROM tm_features), 1));
SELECT setval(pg_get_serial_sequence('tm_role_features', 'id'), COALESCE((SELECT MAX(id) FROM tm_role_features), 1));
