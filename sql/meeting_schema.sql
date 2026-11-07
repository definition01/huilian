CREATE DATABASE IF NOT EXISTS `tengxunmeeting`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `tengxunmeeting`;

CREATE TABLE IF NOT EXISTS `meeting_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_no` VARCHAR(32) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `meeting_user_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备唯一标识',
  `device_type` VARCHAR(32) NOT NULL COMMENT '设备类型: web/ios/android/desktop',
  `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
  `os_name` VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
  `client_version` VARCHAR(32) DEFAULT NULL COMMENT '客户端版本',
  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `last_seen_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
  KEY `idx_device_type` (`device_type`),
  CONSTRAINT `fk_meeting_user_device_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设备表';

CREATE TABLE IF NOT EXISTS `meeting_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_no` VARCHAR(32) NOT NULL COMMENT '会议号',
  `host_user_id` BIGINT NOT NULL COMMENT '主持人用户ID',
  `topic` VARCHAR(128) NOT NULL COMMENT '会议主题',
  `room_type` VARCHAR(32) NOT NULL DEFAULT 'instant' COMMENT '会议类型: instant/scheduled/pmi',
  `password` VARCHAR(32) DEFAULT NULL COMMENT '入会密码',
  `join_link` VARCHAR(512) DEFAULT NULL COMMENT '入会链接',
  `status` VARCHAR(32) NOT NULL DEFAULT 'waiting' COMMENT '会议状态: waiting/ongoing/ended/cancelled',
  `max_participant_count` INT NOT NULL DEFAULT 100 COMMENT '最大参会人数',
  `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '预约开始时间',
  `scheduled_end_time` DATETIME DEFAULT NULL COMMENT '预约结束时间',
  `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
  `mute_on_entry` TINYINT NOT NULL DEFAULT 0 COMMENT '入会静音',
  `allow_screen_share` TINYINT NOT NULL DEFAULT 1 COMMENT '允许共享屏幕',
  `allow_recording` TINYINT NOT NULL DEFAULT 1 COMMENT '允许录制',
  `waiting_room_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '开启等候室',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_no` (`room_no`),
  KEY `idx_host_status` (`host_user_id`, `status`),
  KEY `idx_status_start_time` (`status`, `scheduled_start_time`),
  CONSTRAINT `fk_meeting_room_host` FOREIGN KEY (`host_user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议房间表';

CREATE TABLE IF NOT EXISTS `meeting_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID, 游客可为空',
  `display_name` VARCHAR(64) NOT NULL COMMENT '会议内显示名',
  `role_type` VARCHAR(32) NOT NULL DEFAULT 'participant' COMMENT '角色: host/co_host/participant/guest',
  `join_source` VARCHAR(32) NOT NULL DEFAULT 'app' COMMENT '入会来源: app/web/mobile',
  `join_status` VARCHAR(32) NOT NULL DEFAULT 'joined' COMMENT '状态: waiting/joined/left/kicked/rejected',
  `join_time` DATETIME DEFAULT NULL COMMENT '入会时间',
  `leave_time` DATETIME DEFAULT NULL COMMENT '离会时间',
  `leave_reason` VARCHAR(64) DEFAULT NULL COMMENT '离会原因',
  `is_microphone_on` TINYINT NOT NULL DEFAULT 0 COMMENT '麦克风是否开启',
  `is_camera_on` TINYINT NOT NULL DEFAULT 0 COMMENT '摄像头是否开启',
  `is_screen_sharing` TINYINT NOT NULL DEFAULT 0 COMMENT '是否共享屏幕',
  `is_hand_raised` TINYINT NOT NULL DEFAULT 0 COMMENT '是否举手',
  `network_quality` VARCHAR(32) DEFAULT NULL COMMENT '网络质量',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_join_status` (`room_id`, `join_status`),
  KEY `idx_user_room` (`user_id`, `room_id`),
  CONSTRAINT `fk_meeting_participant_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_participant_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参会人表';

CREATE TABLE IF NOT EXISTS `meeting_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号',
  `room_id` BIGINT DEFAULT NULL COMMENT '会议ID',
  `participant_id` BIGINT DEFAULT NULL COMMENT '参会人ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '设备ID',
  `netty_channel_id` VARCHAR(64) DEFAULT NULL COMMENT 'Netty Channel ID',
  `client_type` VARCHAR(32) NOT NULL DEFAULT 'app' COMMENT '客户端类型',
  `connect_status` VARCHAR(32) NOT NULL DEFAULT 'connected' COMMENT '连接状态: connected/disconnected/reconnected',
  `connected_at` DATETIME DEFAULT NULL COMMENT '连接时间',
  `last_heartbeat_at` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `disconnected_at` DATETIME DEFAULT NULL COMMENT '断开时间',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_room_status` (`room_id`, `connect_status`),
  KEY `idx_user_status` (`user_id`, `connect_status`),
  CONSTRAINT `fk_meeting_session_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_session_participant` FOREIGN KEY (`participant_id`) REFERENCES `meeting_participant` (`id`),
  CONSTRAINT `fk_meeting_session_user` FOREIGN KEY (`user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信令会话表';

CREATE TABLE IF NOT EXISTS `meeting_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `sender_participant_id` BIGINT NOT NULL COMMENT '发送者参会人ID',
  `message_type` VARCHAR(32) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/system/file',
  `content` TEXT COMMENT '消息内容',
  `target_scope` VARCHAR(32) NOT NULL DEFAULT 'all' COMMENT '目标范围: all/private/host',
  `target_participant_id` BIGINT DEFAULT NULL COMMENT '私聊目标参会人ID',
  `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_sent_at` (`room_id`, `sent_at`),
  KEY `idx_sender_sent_at` (`sender_participant_id`, `sent_at`),
  CONSTRAINT `fk_meeting_chat_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_chat_sender` FOREIGN KEY (`sender_participant_id`) REFERENCES `meeting_participant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议聊天消息表';

CREATE TABLE IF NOT EXISTS `meeting_invitation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `inviter_user_id` BIGINT NOT NULL COMMENT '邀请人ID',
  `invitee_user_id` BIGINT DEFAULT NULL COMMENT '受邀用户ID',
  `invitee_contact` VARCHAR(128) DEFAULT NULL COMMENT '受邀联系方式',
  `invite_channel` VARCHAR(32) NOT NULL DEFAULT 'link' COMMENT '邀请渠道: link/mobile/email',
  `invite_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '邀请状态: pending/accepted/rejected/expired',
  `expired_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `responded_at` DATETIME DEFAULT NULL COMMENT '响应时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_invite_status` (`room_id`, `invite_status`),
  KEY `idx_invitee_status` (`invitee_user_id`, `invite_status`),
  CONSTRAINT `fk_meeting_invitation_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_invitation_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `meeting_user` (`id`),
  CONSTRAINT `fk_meeting_invitation_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议邀请表';

CREATE TABLE IF NOT EXISTS `meeting_recording` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` BIGINT NOT NULL COMMENT '会议ID',
  `operator_user_id` BIGINT NOT NULL COMMENT '录制操作人',
  `recording_type` VARCHAR(32) NOT NULL DEFAULT 'cloud' COMMENT '录制类型: local/cloud',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  `file_url` VARCHAR(512) DEFAULT NULL COMMENT '文件地址',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小字节数',
  `recording_status` VARCHAR(32) NOT NULL DEFAULT 'processing' COMMENT '录制状态: processing/completed/failed',
  `started_at` DATETIME DEFAULT NULL COMMENT '录制开始时间',
  `ended_at` DATETIME DEFAULT NULL COMMENT '录制结束时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_recording_status` (`room_id`, `recording_status`),
  CONSTRAINT `fk_meeting_recording_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`),
  CONSTRAINT `fk_meeting_recording_user` FOREIGN KEY (`operator_user_id`) REFERENCES `meeting_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议录制表';
