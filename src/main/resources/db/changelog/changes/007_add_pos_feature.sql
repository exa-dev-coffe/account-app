-- liquibase formatted sql

-- changeset developer:7
-- Add pos (Point of Sale) feature and assign full permissions to default admin (role_id 1) and barista (role_id 3)

INSERT INTO tm_features (feature_id, feature_key, feature_name, description)
VALUES (12, 'pos', 'Point of Sale (POS)', 'Cashier POS order terminal with Takeaway, Dine-in, Cash and Midtrans payment')
ON CONFLICT (feature_id) DO NOTHING;

-- Grant to Admin (role 1)
INSERT INTO tm_role_features (role_id, feature_id, can_view, can_create, can_edit, can_delete)
VALUES (1, 12, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (role_id, feature_id) DO NOTHING;

-- Grant to Barista (role 3)
INSERT INTO tm_role_features (role_id, feature_id, can_view, can_create, can_edit, can_delete)
VALUES (3, 12, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (role_id, feature_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('tm_features', 'feature_id'), COALESCE((SELECT MAX(feature_id) FROM tm_features), 1));
SELECT setval(pg_get_serial_sequence('tm_role_features', 'id'), COALESCE((SELECT MAX(id) FROM tm_role_features), 1));
