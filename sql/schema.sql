-- =============================================================
-- 会联（huilian）数据库 Schema（唯一权威脚本）
-- 数据库：tengxunmeeting
-- 说明：仅包含表结构，不包含任何初始化数据
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `tengxunmeeting`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `tengxunmeeting`;

-- ------------------------------------------------------------
-- 表：meeting_user
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `password` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `mobile` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '头像地址',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '用户状态: 1-启用, 0-禁用',
  `enabled` TINYINT NOT NULL DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `role` VARCHAR(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '0|普通用户 1|管理员',
  `meeting_id` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '正在进行的会议id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_user_mobile` (`mobile`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_status_deleted` (`enabled`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 表：meeting_user_device
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_user_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `device_id` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备唯一标识',
  `device_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备类型: web/ios/android/desktop',
  `device_name` VARCHAR(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备名称',
  `os_name` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统',
  `client_version` VARCHAR(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端版本',
  `last_login_ip` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `last_seen_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`,`device_id`),
  KEY `idx_device_type` (`device_type`),
  CONSTRAINT `fk_meeting_user_device_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设备表';

-- ------------------------------------------------------------
-- 表：meeting_room
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_number` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会议号',
  `topic` VARCHAR(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议主题',
  `host_user_id` BIGINT NOT NULL COMMENT '主持人用户ID',
  `room_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'instant' COMMENT '会议类型: instant/scheduled',
  `password` VARCHAR(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入会密码',
  `join_link` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入会链接',
  `status` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'waiting' COMMENT '会议状态: waiting/ongoing/ended/cancelled',
  `max_participant_count` INT NOT NULL DEFAULT '100' COMMENT '最大参会人数',
  `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '预约开始时间',
  `scheduled_end_time` DATETIME DEFAULT NULL COMMENT '预约结束时间',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration` INT DEFAULT NULL COMMENT '预计持续时长（分钟）',
  `description` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议描述',
  `mute_on_entry` TINYINT NOT NULL DEFAULT '0' COMMENT '入会静音',
  `allow_screen_share` TINYINT NOT NULL DEFAULT '1' COMMENT '允许共享屏幕',
  `waiting_room_enabled` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否开启等候室: 0-关闭, 1-开启',
  `record_enabled` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否开启录制: 0-关闭, 1-开启',
  `auto_record` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否自动录制: 0-关闭, 1-开启',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_no` (`meeting_number`),
  KEY `idx_host_status` (`host_user_id`,`status`),
  KEY `idx_status_start_time` (`status`,`scheduled_start_time`),
  CONSTRAINT `fk_meeting_room_host` FOREIGN KEY (`host_user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议房间表';

-- ------------------------------------------------------------
-- 表：meeting_participant
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID, 游客可为空',
  `display_name` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会议内显示名',
  `role_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'participant' COMMENT '角色: host/co_host/participant/guest',
  `join_source` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'app' COMMENT '入会来源: app/web/mobile',
  `join_status` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'joined' COMMENT '状态: waiting/joined/left/kicked/rejected',
  `join_time` DATETIME DEFAULT NULL COMMENT '入会时间',
  `leave_time` DATETIME DEFAULT NULL COMMENT '离会时间',
  `leave_reason` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '离会原因',
  `is_microphone_on` TINYINT NOT NULL DEFAULT '0' COMMENT '麦克风是否开启',
  `is_camera_on` TINYINT NOT NULL DEFAULT '0' COMMENT '摄像头是否开启',
  `is_screen_sharing` TINYINT NOT NULL DEFAULT '0' COMMENT '是否共享屏幕',
  `is_hand_raised` TINYINT NOT NULL DEFAULT '0' COMMENT '是否举手',
  `network_quality` VARCHAR(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '网络质量',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_join_status` (`room_id`,`join_status`),
  KEY `idx_user_room` (`user_id`,`room_id`),
  CONSTRAINT `fk_meeting_participant_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_participant_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参会人表';

-- ------------------------------------------------------------
-- 表：meeting_session
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_no` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话编号',
  `room_id` BIGINT DEFAULT NULL COMMENT '会议ID',
  `participant_id` BIGINT DEFAULT NULL COMMENT '参会人ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `device_id` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备ID',
  `netty_channel_id` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Netty Channel ID',
  `client_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'app' COMMENT '客户端类型',
  `connect_status` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'connected' COMMENT '连接状态: connected/disconnected/reconnected',
  `connected_at` DATETIME DEFAULT NULL COMMENT '连接时间',
  `last_heartbeat_at` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `disconnected_at` DATETIME DEFAULT NULL COMMENT '断开时间',
  `client_ip` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_room_status` (`room_id`,`connect_status`),
  KEY `idx_user_status` (`user_id`,`connect_status`),
  KEY `fk_meeting_session_participant` (`participant_id`),
  CONSTRAINT `fk_meeting_session_participant` FOREIGN KEY (`participant_id`) REFERENCES `meeting_participant` (`id`),
  CONSTRAINT `fk_meeting_session_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_session_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信令会话表';

-- ------------------------------------------------------------
-- 表：meeting_chat_message
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT DEFAULT NULL COMMENT '会议ID',
  `sender_name` VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息发送者名称',
  `sender_id` BIGINT NOT NULL COMMENT '发送者参会人ID',
  `message_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT/system/file',
  `content` TEXT COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `target_scope` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'all' COMMENT '目标范围: group/private/host',
  `receved_id` BIGINT NOT NULL COMMENT '私聊目标参会人ID',
  `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reply_to_id` BIGINT DEFAULT NULL COMMENT '引用消息id',
  PRIMARY KEY (`id`),
  KEY `idx_room_sent_at` (`meeting_id`,`sent_at`),
  KEY `idx_sender_sent_at` (`sender_id`,`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议聊天消息表';

-- ------------------------------------------------------------
-- 表：meeting_transcript
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_transcript` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT NOT NULL COMMENT '发言人用户ID',
  `user_name` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发言人名称',
  `text_content` TEXT COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '识别的文本内容',
  `start_time` INT NOT NULL COMMENT '发言开始时间（相对会议开始的秒数）',
  `end_time` INT NOT NULL COMMENT '发言结束时间（相对会议开始的秒数）',
  `confidence` DOUBLE DEFAULT '1' COMMENT '识别置信度 0-1',
  `is_final` TINYINT(1) DEFAULT '1' COMMENT '是否为最终结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_meeting_final` (`meeting_id`,`is_final`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议语音转文字记录表';

-- ------------------------------------------------------------
-- 表：meeting_invitation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_invitation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `inviter_user_id` BIGINT NOT NULL COMMENT '邀请人ID',
  `invitee_user_id` BIGINT DEFAULT NULL COMMENT '受邀用户ID',
  `invitee_contact` VARCHAR(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '受邀联系方式',
  `invite_channel` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'link' COMMENT '邀请渠道: link/mobile/email',
  `invite_status` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '邀请状态: pending/accepted/rejected/expired',
  `expired_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `responded_at` DATETIME DEFAULT NULL COMMENT '响应时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_invite_status` (`room_id`,`invite_status`),
  KEY `idx_invitee_status` (`invitee_user_id`,`invite_status`),
  KEY `fk_meeting_invitation_inviter` (`inviter_user_id`),
  CONSTRAINT `fk_meeting_invitation_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `meeting_user` (`id`),
  CONSTRAINT `fk_meeting_invitation_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `meeting_user` (`id`),
  CONSTRAINT `fk_meeting_invitation_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议邀请表';

-- ------------------------------------------------------------
-- 表：meeting_recording
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_recording` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `operator_user_id` BIGINT NOT NULL COMMENT '录制操作人',
  `recording_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'cloud' COMMENT '录制类型: local/cloud',
  `file_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `file_url` VARCHAR(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件地址',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小字节数',
  `recording_status` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'processing' COMMENT '录制状态: processing/completed/failed',
  `started_at` DATETIME DEFAULT NULL COMMENT '录制开始时间',
  `ended_at` DATETIME DEFAULT NULL COMMENT '录制结束时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_recording_status` (`room_id`,`recording_status`),
  KEY `fk_meeting_recording_user` (`operator_user_id`),
  CONSTRAINT `fk_meeting_recording_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_recording_user` FOREIGN KEY (`operator_user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议录制表';

-- ------------------------------------------------------------
-- 表：meeting_ai_settings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_ai_settings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `translation_enabled` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否开启实时翻译',
  `source_language` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'auto' COMMENT '源语言，auto 表示自动识别',
  `target_languages` JSON DEFAULT NULL COMMENT '目标语言数组，如 ["en","ja"]',
  `summary_enabled` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '是否允许生成AI总结',
  `summary_language` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh' COMMENT '总结输出语言',
  `auto_summary` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '会议结束后是否自动生成总结',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting` (`meeting_id`),
  CONSTRAINT `fk_ai_settings_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议AI配置表';

-- ------------------------------------------------------------
-- 表：meeting_ai_task
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_ai_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `task_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型: SUMMARY/TRANSLATION_BACKFILL',
  `status` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
  `retry_count` INT NOT NULL DEFAULT '0' COMMENT '重试次数',
  `payload` JSON DEFAULT NULL COMMENT '任务参数',
  `result_ref_id` BIGINT DEFAULT NULL COMMENT '结果ID（如总结ID）',
  `error_message` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '结束时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_type_status` (`meeting_id`,`task_type`,`status`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_ai_task_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议AI异步任务表';

-- ------------------------------------------------------------
-- 表：meeting_summary
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `summary_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'full' COMMENT '总结类型: full/quick',
  `language` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh' COMMENT '输出语言',
  `title` VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '总结标题',
  `overview` TEXT COLLATE utf8mb4_unicode_ci COMMENT '会议概述',
  `key_points` JSON DEFAULT NULL COMMENT '关键要点数组',
  `decisions` JSON DEFAULT NULL COMMENT '决议事项数组',
  `action_items` JSON DEFAULT NULL COMMENT '待办事项数组（含负责人/截止时间）',
  `keywords` JSON DEFAULT NULL COMMENT '关键词数组',
  `model` VARCHAR(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用的模型',
  `prompt_tokens` INT DEFAULT NULL COMMENT '输入token数',
  `completion_tokens` INT DEFAULT NULL COMMENT '输出token数',
  `status` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态: pending/generating/success/failed',
  `trigger_type` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'auto' COMMENT '触发方式: auto/manual',
  `error_message` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `generated_at` DATETIME DEFAULT NULL COMMENT '生成完成时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_status` (`meeting_id`,`status`),
  CONSTRAINT `fk_summary_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会议总结表';

-- ------------------------------------------------------------
-- 表：meeting_translation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_translation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `transcript_id` BIGINT DEFAULT NULL COMMENT '对应转录记录ID',
  `user_id` BIGINT NOT NULL COMMENT '发言人用户ID',
  `user_name` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发言人名称',
  `segment_index` INT NOT NULL DEFAULT '0' COMMENT '会议内有序片段号',
  `source_language` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'auto' COMMENT '识别出的源语言',
  `target_language` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标语言',
  `source_text` TEXT COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原文',
  `translated_text` TEXT COLLATE utf8mb4_unicode_ci COMMENT '译文',
  `engine` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'llm' COMMENT '翻译引擎: llm/translate',
  `latency_ms` INT DEFAULT NULL COMMENT '翻译耗时（毫秒）',
  `status` VARCHAR(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'success' COMMENT '状态: success/failed',
  `is_final` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '是否最终结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_lang` (`meeting_id`,`target_language`,`id`),
  KEY `idx_transcript` (`transcript_id`),
  CONSTRAINT `fk_translation_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议实时翻译记录表';

-- ------------------------------------------------------------
-- 表：im_friend
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_friend` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `friend_id` BIGINT NOT NULL,
  `remark` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系';

-- ------------------------------------------------------------
-- 表：im_friend_request
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_friend_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL,
  `to_user_id` BIGINT NOT NULL,
  `apply_message` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_from_to` (`from_user_id`,`to_user_id`),
  KEY `idx_to_status` (`to_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友申请';

-- ------------------------------------------------------------
-- 表：im_private_message
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_private_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL,
  `to_user_id` BIGINT NOT NULL,
  `message_type` VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  `content` VARCHAR(2000) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_from_to` (`from_user_id`,`to_user_id`,`id`),
  KEY `idx_to_from` (`to_user_id`,`from_user_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私聊消息';

-- ------------------------------------------------------------
-- 表：im_group
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_name` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群名称',
  `group_no` VARCHAR(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群号(10位数字)',
  `owner_id` BIGINT NOT NULL COMMENT '群主用户ID',
  `avatar` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群头像',
  `announcement` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群公告',
  `status` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT '群状态: normal-正常, banned-已封禁',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_no` (`group_no`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ------------------------------------------------------------
-- 表：im_group_member
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_type` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'member' COMMENT '角色: owner/admin/member',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ------------------------------------------------------------
-- 表：im_group_message
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_group_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
  `sender_name` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发送者名称',
  `message_type` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT/SYSTEM',
  `content` TEXT COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `file_url` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件访问地址',
  `file_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件原始名称',
  `file_size` BIGINT DEFAULT NULL COMMENT '附件大小(字节)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_group_id_id` (`group_id`,`id`),
  KEY `idx_sender_id` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';

-- ------------------------------------------------------------
-- 表：im_group_join_request
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_group_join_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `inviter_id` BIGINT DEFAULT NULL COMMENT '邀请人用户ID，NULL表示主动申请',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_group_status` (`group_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群聊入群申请';

-- ------------------------------------------------------------
-- 表：im_group_appeal
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_group_appeal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `owner_id` BIGINT NOT NULL COMMENT '申诉人（群主）用户ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '申诉理由',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  `admin_remark` VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群聊封禁申诉';

-- ------------------------------------------------------------
-- 表：im_chat_state
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `im_chat_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `conversation_type` VARCHAR(16) NOT NULL COMMENT '会话类型: GROUP/PRIVATE',
  `target_id` BIGINT NOT NULL COMMENT '会话目标ID（群ID或好友用户ID）',
  `last_read_message_id` BIGINT NOT NULL DEFAULT '0' COMMENT '已读到的最后一条消息ID',
  `pinned` TINYINT NOT NULL DEFAULT '0' COMMENT '是否置顶: 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_conv` (`user_id`,`conversation_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话已读与置顶状态';

-- ------------------------------------------------------------
-- 表：fi_operation_logs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fi_operation_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` VARCHAR(100) NOT NULL COMMENT '操作类型',
  `description` TEXT COMMENT '操作描述',
  `user_id` BIGINT DEFAULT NULL COMMENT '执行用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '执行用户名',
  `operator` VARCHAR(100) DEFAULT NULL COMMENT '操作人标识',
  `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功: 0-失败, 1-成功',
  `params` TEXT COMMENT '输入参数快照',
  `result` TEXT COMMENT '输出结果快照',
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
