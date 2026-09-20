SET @first_frame_source_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'storyboard_first_frame'
      AND COLUMN_NAME = 'source'
);
SET @first_frame_source_sql := IF(
    @first_frame_source_exists = 0,
    'ALTER TABLE storyboard_first_frame ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''AI'' AFTER status',
    'SELECT 1'
);
PREPARE first_frame_source_statement FROM @first_frame_source_sql;
EXECUTE first_frame_source_statement;
DEALLOCATE PREPARE first_frame_source_statement;

SET @video_source_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'storyboard_video'
      AND COLUMN_NAME = 'source'
);
SET @video_source_sql := IF(
    @video_source_exists = 0,
    'ALTER TABLE storyboard_video ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''AI'' AFTER version',
    'SELECT 1'
);
PREPARE video_source_statement FROM @video_source_sql;
EXECUTE video_source_statement;
DEALLOCATE PREPARE video_source_statement;

SET @video_deleted_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'storyboard_video'
      AND COLUMN_NAME = 'is_deleted'
);
SET @video_deleted_sql := IF(
    @video_deleted_exists = 0,
    'ALTER TABLE storyboard_video ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_current',
    'SELECT 1'
);
PREPARE video_deleted_statement FROM @video_deleted_sql;
EXECUTE video_deleted_statement;
DEALLOCATE PREPARE video_deleted_statement;

UPDATE storyboard_first_frame SET source = 'AI' WHERE source IS NULL OR source = '';
UPDATE storyboard_video SET source = 'AI' WHERE source IS NULL OR source = '';

SET @video_lookup_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'storyboard_video'
      AND INDEX_NAME = 'idx_storyboard_video_available'
);
SET @video_lookup_index_sql := IF(
    @video_lookup_index_exists = 0,
    'CREATE INDEX idx_storyboard_video_available ON storyboard_video (storyboard_scene_id, is_deleted, version)',
    'SELECT 1'
);
PREPARE video_lookup_index_statement FROM @video_lookup_index_sql;
EXECUTE video_lookup_index_statement;
DEALLOCATE PREPARE video_lookup_index_statement;
