ALTER TABLE nso_sample_confirm
    ADD COLUMN max_use_count INT NOT NULL DEFAULT 1,
    ADD COLUMN used_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_sample_confirm_expire ON nso_sample_confirm (expire_at, used_flag);
