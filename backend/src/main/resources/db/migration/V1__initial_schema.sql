CREATE TABLE app_user (
    id CHAR(36) PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE user_session (
    token_hash CHAR(64) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_session_expiry (expires_at)
);

CREATE TABLE template (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    required_image_count INT NOT NULL,
    max_file_size_bytes BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CHECK (required_image_count BETWEEN 1 AND 4),
    CHECK (max_file_size_bytes > 0),
    INDEX idx_template_category (category, sort_order)
);

CREATE TABLE upload_asset (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    mime_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_asset_user (user_id, created_at)
);

CREATE TABLE production_task (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
    current_stage VARCHAR(24),
    failure_stage VARCHAR(24),
    failure_reason VARCHAR(500),
    result_url VARCHAR(500),
    simulation_failure_stage VARCHAR(24),
    simulation_delay_ms INT NOT NULL DEFAULT 2000,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    finished_at TIMESTAMP(3),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (template_id) REFERENCES template(id),
    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CHECK (current_stage IS NULL OR current_stage IN ('VALIDATE', 'GENERATE', 'FINALIZE')),
    CHECK (failure_stage IS NULL OR failure_stage IN ('VALIDATE', 'GENERATE', 'FINALIZE')),
    CHECK (simulation_failure_stage IS NULL OR simulation_failure_stage IN ('VALIDATE', 'GENERATE', 'FINALIZE')),
    CHECK (simulation_delay_ms BETWEEN 0 AND 30000),
    INDEX idx_task_user_created (user_id, created_at, id),
    INDEX idx_task_status (status, created_at)
);

CREATE TABLE task_asset (
    task_id CHAR(36) NOT NULL,
    asset_id CHAR(36) NOT NULL,
    slot_index INT NOT NULL,
    PRIMARY KEY (task_id, slot_index),
    UNIQUE KEY uk_task_asset (task_id, asset_id),
    FOREIGN KEY (task_id) REFERENCES production_task(id),
    FOREIGN KEY (asset_id) REFERENCES upload_asset(id)
);

CREATE TABLE task_step (
    task_id CHAR(36) NOT NULL,
    stage VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at TIMESTAMP(3),
    error_message VARCHAR(500),
    PRIMARY KEY (task_id, stage),
    FOREIGN KEY (task_id) REFERENCES production_task(id),
    CHECK (stage IN ('VALIDATE', 'GENERATE', 'FINALIZE')),
    CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
);
