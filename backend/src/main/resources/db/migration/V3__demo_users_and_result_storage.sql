INSERT INTO app_user (id, display_name) VALUES
('00000000-0000-0000-0000-000000000001', '小拾'),
('00000000-0000-0000-0000-000000000002', '小光');

ALTER TABLE production_task
    ADD COLUMN result_storage_key VARCHAR(255),
    ADD COLUMN result_mime_type VARCHAR(50);
