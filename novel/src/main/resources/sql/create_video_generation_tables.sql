CREATE TABLE IF NOT EXISTS video_generation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    storyboard_scene_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_task_id VARCHAR(128),
    first_frame_path VARCHAR(1000),
    resolution VARCHAR(16),
    error_message TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_video_task_scene (novel_id, chapter_num, storyboard_scene_id),
    KEY idx_video_task_status (status)
);

CREATE TABLE IF NOT EXISTS storyboard_video (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    storyboard_scene_id BIGINT NOT NULL,
    version INT NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'AI',
    video_path VARCHAR(1000) NOT NULL,
    first_frame_path VARCHAR(1000),
    duration_sec INT,
    resolution VARCHAR(16),
    prompt_snapshot LONGTEXT,
    style_snapshot LONGTEXT,
    is_current TINYINT(1) NOT NULL DEFAULT 1,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_storyboard_video_version (storyboard_scene_id, version),
    KEY idx_storyboard_video_scene (novel_id, chapter_num, storyboard_scene_id),
    KEY idx_storyboard_video_available (storyboard_scene_id, is_deleted, version)
);

CREATE TABLE IF NOT EXISTS storyboard_video_asset (
    video_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_version INT NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    image_path VARCHAR(1000) NOT NULL,
    PRIMARY KEY (video_id, asset_id),
    CONSTRAINT fk_storyboard_video_asset_video FOREIGN KEY (video_id) REFERENCES storyboard_video(id)
);
