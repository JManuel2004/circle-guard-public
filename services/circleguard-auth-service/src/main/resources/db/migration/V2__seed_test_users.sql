-- Seed test users for development
-- BCrypt hash for "password"
INSERT INTO local_users (username, password_hash, email, is_active) VALUES 
('staff_guard', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1RphLVCjd07W', 'guard@circleguard.edu', true),
('health_user', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1RphLVCjd07W', 'health@circleguard.edu', true),
('super_admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1RphLVCjd07W', 'admin@circleguard.edu', true);

-- Assign GATE_STAFF role to staff_guard
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM local_users u, roles r 
WHERE u.username = 'staff_guard' AND r.name = 'GATE_STAFF';

-- Assign HEALTH_CENTER role to health_user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM local_users u, roles r 
WHERE u.username = 'health_user' AND r.name = 'HEALTH_CENTER';

-- Assign all roles to super_admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM local_users u, roles r 
WHERE u.username = 'super_admin';
