CREATE TABLE IF NOT EXISTS storyboard_first_frame (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    storyboard_scene_id BIGINT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'AI',
    image_path VARCHAR(1000),
    prompt_snapshot LONGTEXT,
    style_snapshot LONGTEXT,
    error_message TEXT,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_first_frame_version (storyboard_scene_id, version),
    KEY idx_first_frame_scene (novel_id, chapter_num, storyboard_scene_id, is_deleted),
    KEY idx_first_frame_running (storyboard_scene_id, status)
);

SET @first_frame_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'video_generation_task'
      AND COLUMN_NAME = 'first_frame_id'
);
SET @first_frame_alter_sql := IF(
    @first_frame_column_exists = 0,
    'ALTER TABLE video_generation_task ADD COLUMN first_frame_id BIGINT NULL AFTER storyboard_scene_id',
    'SELECT 1'
);
PREPARE first_frame_alter_statement FROM @first_frame_alter_sql;
EXECUTE first_frame_alter_statement;
DEALLOCATE PREPARE first_frame_alter_statement;
