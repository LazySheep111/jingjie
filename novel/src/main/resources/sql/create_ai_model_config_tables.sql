CREATE TABLE IF NOT EXISTS ai_model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    capability_type VARCHAR(16) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    api_url VARCHAR(1000) NOT NULL,
    query_url VARCHAR(1000),
    encrypted_api_key TEXT NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    test_status VARCHAR(32) NOT NULL DEFAULT 'NOT_TESTED',
    last_test_at DATETIME NULL,
    last_error VARCHAR(500),
    version INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_model_config_active (capability_type, enabled, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
