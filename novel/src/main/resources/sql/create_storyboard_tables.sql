CREATE TABLE IF NOT EXISTS novel_storyboard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    total_duration_sec INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_storyboard_novel_chapter (novel_id, chapter_num, version)
);

CREATE TABLE IF NOT EXISTS novel_storyboard_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    storyboard_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    duration_sec INT NOT NULL DEFAULT 10,
    location VARCHAR(255),
    time_of_day VARCHAR(64),
    weather VARCHAR(128),
    characters TEXT,
    shot_type VARCHAR(128),
    camera_movement VARCHAR(255),
    shot_plan TEXT NOT NULL,
    character_emotion VARCHAR(255),
    voice_over TEXT,
    transition VARCHAR(128),
    image_prompt TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_storyboard_scene_storyboard (storyboard_id, sequence)
);
