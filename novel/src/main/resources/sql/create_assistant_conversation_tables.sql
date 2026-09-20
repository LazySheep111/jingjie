CREATE TABLE IF NOT EXISTS assistant_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL UNIQUE,
    novel_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    last_message_preview VARCHAR(500) NULL,
    message_count INT NOT NULL DEFAULT 0,
    conversation_version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_assistant_conversation_updated (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assistant_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(100) NOT NULL UNIQUE,
    conversation_id VARCHAR(100) NOT NULL,
    sequence_no BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    tool_name VARCHAR(100) NULL,
    tool_arguments LONGTEXT NULL,
    tool_result LONGTEXT NULL,
    parent_message_id VARCHAR(100) NULL,
    attempt_no INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    persisted_at DATETIME NULL,
    UNIQUE KEY uk_assistant_message_sequence (conversation_id, sequence_no),
    INDEX idx_assistant_message_conversation (conversation_id, sequence_no DESC),
    CONSTRAINT fk_assistant_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES assistant_conversation (conversation_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
