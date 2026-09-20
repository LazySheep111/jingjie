CREATE TABLE IF NOT EXISTS novel_visual_style (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    era VARCHAR(255),
    region VARCHAR(255),
    architecture TEXT,
    material TEXT,
    color_style TEXT,
    lighting_style TEXT,
    art_style TEXT,
    camera_style TEXT,
    positive_prompt TEXT,
    negative_prompt TEXT,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_visual_style_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
