ALTER TABLE system_settings 
ADD COLUMN mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
ADD COLUMN encounter_window_days INTEGER NOT NULL DEFAULT 14;

-- Seed initial values if not present
UPDATE system_settings SET mandatory_fence_days = 14, encounter_window_days = 14 WHERE mandatory_fence_days IS NULL;
