CREATE TABLE IF NOT EXISTS novel_visual_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    current_version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'PROMPT_READY',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_visual_asset_identity (novel_id, asset_type, normalized_name),
    KEY idx_visual_asset_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_visual_asset_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    version INT NOT NULL,
    core_features TEXT,
    front_prompt TEXT,
    side_prompt TEXT,
    back_prompt TEXT,
    composite_image_path VARCHAR(1024),
    status VARCHAR(32) NOT NULL DEFAULT 'PROMPT_READY',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_visual_asset_version (asset_id, version),
    CONSTRAINT fk_visual_asset_version_asset FOREIGN KEY (asset_id)
        REFERENCES novel_visual_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @asset_image_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'novel_visual_asset_version'
      AND column_name = 'composite_image_path'
);
SET @asset_image_alter_sql = IF(
    @asset_image_column_exists = 0,
    'ALTER TABLE novel_visual_asset_version ADD COLUMN composite_image_path VARCHAR(1024) AFTER back_prompt',
    'SELECT 1'
);
PREPARE asset_image_alter_stmt FROM @asset_image_alter_sql;
EXECUTE asset_image_alter_stmt;
DEALLOCATE PREPARE asset_image_alter_stmt;

CREATE TABLE IF NOT EXISTS novel_composite_image_task (
    id VARCHAR(64) PRIMARY KEY,
    novel_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    image_path VARCHAR(1024),
    error_message TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_composite_image_task_asset (novel_id, asset_id, asset_version, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_storyboard_asset_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    storyboard_scene_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_version INT NOT NULL,
    asset_role VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_storyboard_scene_asset (
        storyboard_scene_id, asset_id, asset_version, asset_role
    ),
    CONSTRAINT fk_storyboard_asset_ref_scene FOREIGN KEY (storyboard_scene_id)
        REFERENCES novel_storyboard_scene(id) ON DELETE CASCADE,
    CONSTRAINT fk_storyboard_asset_ref_asset FOREIGN KEY (asset_id)
        REFERENCES novel_visual_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_asset_extract_task (
    id VARCHAR(64) PRIMARY KEY,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total INT NOT NULL DEFAULT 0,
    reused INT NOT NULL DEFAULT 0,
    created INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    error_message TEXT,
    message TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_asset_task_chapter (novel_id, chapter_num, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
