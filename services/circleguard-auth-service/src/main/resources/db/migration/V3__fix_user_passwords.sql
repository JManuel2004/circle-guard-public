-- Fix test user passwords with valid BCrypt hashes
-- Password is "password"
UPDATE local_users SET password_hash = '$2b$12$91ldmSUu.lSKkxRXuo0GsuVDH8d/OnRXhaFiUNMk5dXvXOytwICxS'
WHERE username IN ('staff_guard', 'health_user', 'super_admin');
