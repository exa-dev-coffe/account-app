-- liquibase formatted sql

-- changeset developer:5 validCheckSum:ANY
-- Remove all feature permissions for role_id 2 (user/customer) as they do not have dashboard access

DELETE FROM tm_role_features WHERE role_id = 2;

-- rollback INSERT INTO tm_role_features (role_id, feature_id, can_view, can_create, can_edit, can_delete) VALUES (2, 1, TRUE, FALSE, FALSE, FALSE), (2, 2, TRUE, FALSE, FALSE, FALSE), (2, 3, TRUE, FALSE, FALSE, FALSE), (2, 4, TRUE, FALSE, FALSE, FALSE), (2, 5, TRUE, FALSE, FALSE, FALSE) ON CONFLICT (role_id, feature_id) DO NOTHING;

