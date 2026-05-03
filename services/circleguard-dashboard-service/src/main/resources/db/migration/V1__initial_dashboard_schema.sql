-- Dashboard Analytics Schema

CREATE TABLE entry_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL,
    entry_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(10) -- GREEN/RED
);

CREATE INDEX idx_entry_location_time ON entry_logs(location_id, entry_time);
