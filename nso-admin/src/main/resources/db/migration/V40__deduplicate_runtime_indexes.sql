-- Remove the duplicate lifecycle index introduced by the historical V21/V22 pair.
-- V1-V39 remain immutable; this is safe on fresh and upgraded pilot databases.
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user'
      AND index_name = 'idx_sys_user_directory'
);
SET @drop_sql := IF(@idx_exists > 0, 'DROP INDEX idx_sys_user_directory ON sys_user', 'SELECT 1');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
