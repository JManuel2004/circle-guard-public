-- Add priority alerting permission
INSERT INTO permissions (name, description) VALUES 
('alert:receive_priority', 'Receive priority alerts for large outbreaks or new confirmed cases');

-- Map HEALTH_CENTER to the new permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'HEALTH_CENTER' AND p.name = 'alert:receive_priority';
