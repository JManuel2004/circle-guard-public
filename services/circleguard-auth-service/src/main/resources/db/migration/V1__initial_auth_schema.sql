-- Dynamic RBAC Schema

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL, -- e.g. 'gate:scan', 'identity:lookup'
    description TEXT
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL, -- e.g. 'STUDENT', 'HEALTH_CENTER'
    description TEXT
);

CREATE TABLE role_permissions (
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE local_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES local_users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed basic data
INSERT INTO permissions (name, description) VALUES 
('gate:scan', 'Allow scanning QR codes at entry gates'),
('identity:lookup', 'Allow looking up real identity from anonymous ID'),
('circle:checkin', 'Allow checking into circles'),
('symptom:report', 'Allow reporting symptoms');

INSERT INTO roles (name, description) VALUES 
('HEALTH_CENTER', 'University Health Center Personnel'),
('GATE_STAFF', 'Campus Security and Gate Guards'),
('STUDENT', 'University Students');

-- Map HEALTH_CENTER to lookup and symptom management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'HEALTH_CENTER' AND p.name IN ('identity:lookup', 'symptom:report');
