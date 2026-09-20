-- 将旧分镜内容字段收敛为单一镜头脚本字段。
-- 本脚本按已确认方案处理：不迁移旧字段数据，删除没有 shot_plan 的旧分镜。

ALTER TABLE novel_storyboard_scene
    ADD COLUMN shot_plan TEXT NULL AFTER camera_movement;

DELETE FROM novel_storyboard_scene
WHERE shot_plan IS NULL OR TRIM(shot_plan) = '';

DELETE b
FROM novel_storyboard b
LEFT JOIN novel_storyboard_scene s ON s.storyboard_id = b.id
WHERE s.id IS NULL;

ALTER TABLE novel_storyboard_scene
    MODIFY COLUMN shot_plan TEXT NOT NULL,
    DROP COLUMN visual_description,
    DROP COLUMN character_action,
    DROP COLUMN dialogue,
    DROP COLUMN sound_effect,
    DROP COLUMN background_music,
    DROP COLUMN video_prompt;
