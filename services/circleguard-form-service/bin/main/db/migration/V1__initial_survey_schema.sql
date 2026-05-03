-- Health Survey Schema

CREATE TABLE health_surveys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anonymous_id UUID NOT NULL,
    has_fever BOOLEAN DEFAULT FALSE,
    has_cough BOOLEAN DEFAULT FALSE,
    other_symptoms TEXT,
    exposure_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_survey_user ON health_surveys(anonymous_id);
