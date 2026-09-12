-- 原型模式后的身份加固。既有 V1/V2/V3 迁移保持不可变。
ALTER TABLE sys_user
    ADD COLUMN wechat_open_id VARCHAR(128),
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX uk_sys_user_wechat_open_id ON sys_user (wechat_open_id);
