-- Add responses to health_surveys
ALTER TABLE health_surveys ADD COLUMN responses JSONB;
