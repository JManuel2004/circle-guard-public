CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    unconfirmed_fencing_enabled BOOLEAN NOT NULL DEFAULT false,
    auto_threshold_seconds BIGINT NOT NULL DEFAULT 300,
    mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
    encounter_window_days INTEGER NOT NULL DEFAULT 14
);

INSERT INTO system_settings (unconfirmed_fencing_enabled, auto_threshold_seconds, mandatory_fence_days, encounter_window_days)
VALUES (false, 300, 14, 14);
