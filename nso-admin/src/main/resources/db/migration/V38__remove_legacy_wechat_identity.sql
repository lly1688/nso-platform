-- Remove the retired external identity mapping while tolerating partially initialized databases.
SET @nso_wechat_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'uk_sys_user_wechat_open_id'
);
SET @nso_drop_wechat_index = IF(
    @nso_wechat_index_exists > 0,
    'ALTER TABLE sys_user DROP INDEX uk_sys_user_wechat_open_id',
    'SELECT 1'
);
PREPARE nso_drop_wechat_index_stmt FROM @nso_drop_wechat_index;
EXECUTE nso_drop_wechat_index_stmt;
DEALLOCATE PREPARE nso_drop_wechat_index_stmt;

SET @nso_wechat_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'wechat_open_id'
);
SET @nso_drop_wechat_column = IF(
    @nso_wechat_column_exists > 0,
    'ALTER TABLE sys_user DROP COLUMN wechat_open_id',
    'SELECT 1'
);
PREPARE nso_drop_wechat_column_stmt FROM @nso_drop_wechat_column;
EXECUTE nso_drop_wechat_column_stmt;
DEALLOCATE PREPARE nso_drop_wechat_column_stmt;
