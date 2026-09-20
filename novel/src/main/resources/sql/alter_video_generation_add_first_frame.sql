ALTER TABLE video_generation_task
    ADD COLUMN first_frame_path VARCHAR(1000) NULL AFTER provider_task_id;

ALTER TABLE storyboard_video
    ADD COLUMN first_frame_path VARCHAR(1000) NULL AFTER video_path;
