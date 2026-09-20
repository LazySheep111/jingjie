-- Add progress text for existing asset extraction task tables.
-- Use information_schema because MySQL 8.0.31 does not support
-- ADD COLUMN IF NOT EXISTS.
SET @message_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'novel_asset_extract_task'
      AND column_name = 'message'
);

SET @alter_message_sql = IF(
    @message_column_exists = 0,
    'ALTER TABLE novel_asset_extract_task ADD COLUMN message TEXT AFTER error_message',
    'SELECT 1'
);

PREPARE alter_message_stmt FROM @alter_message_sql;
EXECUTE alter_message_stmt;
DEALLOCATE PREPARE alter_message_stmt;
