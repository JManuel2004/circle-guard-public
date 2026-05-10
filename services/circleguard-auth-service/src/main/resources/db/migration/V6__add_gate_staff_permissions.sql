-- Grant GATE_STAFF the permissions needed for circle check-in, symptom reporting, and gate scanning
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'GATE_STAFF'
  AND p.name IN ('circle:checkin', 'symptom:report', 'gate:scan')
ON CONFLICT (role_id, permission_id) DO NOTHING;
