-- V1.0 follow-up: direct impact feedback is an independently editable business action.
-- Preserve V1-V7 checksums and give existing impact rows a deterministic initial version.
ALTER TABLE nso_change_impact
    ADD COLUMN version INT NOT NULL DEFAULT 0;
