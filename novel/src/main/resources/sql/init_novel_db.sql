-- 境界项目 MySQL 初始化脚本（MySQL 8.x）
-- 新环境执行：自动创建并切换到 novel_db，仅补建不存在的表。
-- 本脚本不包含业务数据，不会删除或覆盖已有表。

CREATE DATABASE IF NOT EXISTS novel_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE novel_db;

-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: novel_db
-- ------------------------------------------------------
-- Server version	8.0.31

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_model_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capability_type` varchar(16) NOT NULL,
  `provider_type` varchar(32) NOT NULL,
  `api_url` varchar(1000) NOT NULL,
  `query_url` varchar(1000) DEFAULT NULL,
  `encrypted_api_key` text NOT NULL,
  `model_name` varchar(255) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `test_status` varchar(32) NOT NULL DEFAULT 'NOT_TESTED',
  `last_test_at` datetime DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_model_config_active` (`capability_type`,`enabled`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assistant_conversation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `assistant_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(100) NOT NULL,
  `novel_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `last_message_preview` varchar(500) DEFAULT NULL,
  `message_count` int NOT NULL DEFAULT '0',
  `conversation_version` bigint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `conversation_id` (`conversation_id`),
  UNIQUE KEY `novel_id` (`novel_id`),
  KEY `idx_assistant_conversation_updated` (`updated_at` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assistant_message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `assistant_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message_id` varchar(100) NOT NULL,
  `conversation_id` varchar(100) NOT NULL,
  `sequence_no` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` longtext NOT NULL,
  `status` varchar(30) NOT NULL,
  `tool_name` varchar(100) DEFAULT NULL,
  `tool_arguments` longtext,
  `tool_result` longtext,
  `parent_message_id` varchar(100) DEFAULT NULL,
  `attempt_no` int NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `persisted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `message_id` (`message_id`),
  UNIQUE KEY `uk_assistant_message_sequence` (`conversation_id`,`sequence_no`),
  KEY `idx_assistant_message_conversation` (`conversation_id`,`sequence_no` DESC),
  CONSTRAINT `fk_assistant_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `assistant_conversation` (`conversation_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_asset_extract_task`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_asset_extract_task` (
  `id` varchar(64) NOT NULL,
  `novel_id` bigint NOT NULL,
  `chapter_num` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `total` int NOT NULL DEFAULT '0',
  `reused` int NOT NULL DEFAULT '0',
  `created` int NOT NULL DEFAULT '0',
  `failed` int NOT NULL DEFAULT '0',
  `error_message` text,
  `message` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_asset_task_chapter` (`novel_id`,`chapter_num`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_chapter_text`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_chapter_text` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `novel_id` bigint NOT NULL COMMENT '小说ID',
  `chapter_num` int NOT NULL COMMENT '章节序号',
  `chapter_title` varchar(255) NOT NULL COMMENT '章节标题',
  `chapter_summary` text COMMENT '章节概要',
  `chapter_text` longtext COMMENT '章节正文',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_novel_chapter` (`novel_id`,`chapter_num`),
  KEY `idx_novel_id` (`novel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=237 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说章节正文表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_composite_image_task`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_composite_image_task` (
  `id` varchar(64) NOT NULL,
  `novel_id` bigint NOT NULL,
  `asset_id` bigint NOT NULL,
  `asset_version` int NOT NULL,
  `status` varchar(32) NOT NULL,
  `image_path` varchar(1024) DEFAULT NULL,
  `error_message` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_composite_image_task_asset` (`novel_id`,`asset_id`,`asset_version`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `novel_title` varchar(255) DEFAULT NULL COMMENT '小说标题，用户可以为空由AI生成',
  `category` varchar(64) NOT NULL COMMENT '小说类型：都市、古风仙侠、悬疑等',
  `novel_length` varchar(32) NOT NULL COMMENT '篇幅：短篇、中篇、长篇',
  `ending_type` varchar(32) NOT NULL COMMENT '结局：HE、BE、开放式结局',
  `writing_style` varchar(100) NOT NULL COMMENT '文风设定',
  `target_audience` varchar(64) NOT NULL COMMENT '目标读者',
  `protagonist` text NOT NULL COMMENT '主角设定',
  `role_list` json DEFAULT NULL COMMENT '配角列表JSON字符串',
  `background` varchar(255) NOT NULL COMMENT '时代背景',
  `world_rule` text COMMENT '世界设定，现代文为空',
  `theme` varchar(255) NOT NULL COMMENT '核心主题',
  `trigger_event` text COMMENT '开篇触发事件',
  `foreshadow_count` int DEFAULT '4' COMMENT '伏笔数量',
  `narrative_view` varchar(64) NOT NULL COMMENT '叙事视角',
  `avoid_content` text COMMENT '规避内容',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户提交小说提示词信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_storyboard`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_storyboard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `chapter_num` bigint NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPLETED',
  `total_duration_sec` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_storyboard_novel_chapter` (`novel_id`,`chapter_num`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_storyboard_asset_ref`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_storyboard_asset_ref` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `storyboard_scene_id` bigint NOT NULL,
  `asset_id` bigint NOT NULL,
  `asset_version` int NOT NULL,
  `asset_role` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storyboard_scene_asset` (`storyboard_scene_id`,`asset_id`,`asset_version`,`asset_role`),
  KEY `fk_storyboard_asset_ref_asset` (`asset_id`),
  CONSTRAINT `fk_storyboard_asset_ref_asset` FOREIGN KEY (`asset_id`) REFERENCES `novel_visual_asset` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_storyboard_asset_ref_scene` FOREIGN KEY (`storyboard_scene_id`) REFERENCES `novel_storyboard_scene` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=361 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_storyboard_scene`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_storyboard_scene` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `storyboard_id` bigint NOT NULL,
  `sequence` int NOT NULL,
  `duration_sec` int NOT NULL DEFAULT '10',
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time_of_day` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weather` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `characters` text COLLATE utf8mb4_unicode_ci,
  `shot_type` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `camera_movement` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shot_plan` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `character_emotion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `voice_over` text COLLATE utf8mb4_unicode_ci,
  `transition` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_prompt` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_storyboard_scene_storyboard` (`storyboard_id`,`sequence`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_visual_asset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_visual_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `asset_type` varchar(32) NOT NULL,
  `normalized_name` varchar(255) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `current_version` int NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL DEFAULT 'PROMPT_READY',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visual_asset_identity` (`novel_id`,`asset_type`,`normalized_name`),
  KEY `idx_visual_asset_novel` (`novel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_visual_asset_version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_visual_asset_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint NOT NULL,
  `version` int NOT NULL,
  `core_features` text,
  `front_prompt` text,
  `side_prompt` text,
  `back_prompt` text,
  `composite_image_path` varchar(1024) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PROMPT_READY',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visual_asset_version` (`asset_id`,`version`),
  CONSTRAINT `fk_visual_asset_version_asset` FOREIGN KEY (`asset_id`) REFERENCES `novel_visual_asset` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `novel_visual_style`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `novel_visual_style` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `era` varchar(255) DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `architecture` text,
  `material` text,
  `color_style` text,
  `lighting_style` text,
  `art_style` text,
  `camera_style` text,
  `positive_prompt` text,
  `negative_prompt` text,
  `version` int NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_novel_visual_style_novel` (`novel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `outline`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `outline` (
  `novel_id` bigint NOT NULL AUTO_INCREMENT COMMENT '小说唯一id，主键',
  `novel_title` varchar(200) NOT NULL COMMENT '小说标题',
  `category` varchar(100) NOT NULL COMMENT '小说分类',
  `protagonist` text COMMENT '主角介绍',
  `role_list` json DEFAULT NULL COMMENT '角色列表，数组["妹妹凌溪"]',
  `outline_title` varchar(200) NOT NULL COMMENT '大纲里的小说标题',
  `overall_plot` text COMMENT '整体梗概',
  `foreshadow_list` json DEFAULT NULL COMMENT '伏笔列表',
  `chapter_list` json DEFAULT NULL COMMENT '章节列表',
  `start_full_generation` tinyint(1) DEFAULT '0' COMMENT '是否开启完整生成 false‑0 true‑1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`novel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说大纲表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storyboard_first_frame`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `storyboard_first_frame` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `chapter_num` bigint NOT NULL,
  `storyboard_scene_id` bigint NOT NULL,
  `version` int NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AI',
  `image_path` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prompt_snapshot` longtext COLLATE utf8mb4_unicode_ci,
  `style_snapshot` longtext COLLATE utf8mb4_unicode_ci,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_frame_version` (`storyboard_scene_id`,`version`),
  KEY `idx_first_frame_scene` (`novel_id`,`chapter_num`,`storyboard_scene_id`,`is_deleted`),
  KEY `idx_first_frame_running` (`storyboard_scene_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storyboard_video`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `storyboard_video` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `chapter_num` bigint NOT NULL,
  `storyboard_scene_id` bigint NOT NULL,
  `version` int NOT NULL,
  `source` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AI',
  `video_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_frame_path` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_sec` int DEFAULT NULL,
  `resolution` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prompt_snapshot` longtext COLLATE utf8mb4_unicode_ci,
  `style_snapshot` longtext COLLATE utf8mb4_unicode_ci,
  `is_current` tinyint(1) NOT NULL DEFAULT '1',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storyboard_video_version` (`storyboard_scene_id`,`version`),
  KEY `idx_storyboard_video_scene` (`novel_id`,`chapter_num`,`storyboard_scene_id`),
  KEY `idx_storyboard_video_available` (`storyboard_scene_id`,`is_deleted`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storyboard_video_asset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `storyboard_video_asset` (
  `video_id` bigint NOT NULL,
  `asset_id` bigint NOT NULL,
  `asset_version` int NOT NULL,
  `asset_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `asset_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`video_id`,`asset_id`),
  CONSTRAINT `fk_storyboard_video_asset_video` FOREIGN KEY (`video_id`) REFERENCES `storyboard_video` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `video_generation_task`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `video_generation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `novel_id` bigint NOT NULL,
  `chapter_num` bigint NOT NULL,
  `storyboard_scene_id` bigint NOT NULL,
  `first_frame_id` bigint DEFAULT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_task_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `first_frame_path` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolution` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video_task_scene` (`novel_id`,`chapter_num`,`storyboard_scene_id`),
  KEY `idx_video_task_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-20 17:29:02
