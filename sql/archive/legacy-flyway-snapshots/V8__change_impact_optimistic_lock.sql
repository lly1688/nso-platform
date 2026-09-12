-- V1.0 后续补充：直接影响反馈是一项可独立编辑的业务动作。
-- 保留 V1-V7 校验和，并为既有影响记录赋予确定性的初始版本。
ALTER TABLE nso_change_impact
    ADD COLUMN version INT NOT NULL DEFAULT 0;
