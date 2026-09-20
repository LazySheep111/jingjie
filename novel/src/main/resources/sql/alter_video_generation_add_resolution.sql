ALTER TABLE video_generation_task
    ADD COLUMN resolution VARCHAR(16) NULL AFTER first_frame_path;

ALTER TABLE storyboard_video
    ADD COLUMN resolution VARCHAR(16) NULL AFTER duration_sec;
