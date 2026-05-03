-- Add dashboard viewing permission
INSERT INTO permissions (name, description) VALUES 
('dashboard:view', 'Allow viewing the health analytics dashboard')
ON CONFLICT (name) DO NOTHING;

-- Map HEALTH_CENTER to the new permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'HEALTH_CENTER' AND p.name = 'dashboard:view'
ON CONFLICT (role_id, permission_id) DO NOTHING;
