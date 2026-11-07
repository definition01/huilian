-- =============================================================
-- 腾讯会议 tengxunmeeting 数据库 Schema（统一入口）
--
-- 本文件是唯一权威的建表脚本，覆盖所有实体对应的表：
--   meeting_user          -> User
--   meeting_room          -> Meeting
--   meeting_participant   -> MeetingParticipant
--   meeting_chat_message  -> MessageSend
--   meeting_transcript    -> MeetingTranscript
--   fi_operation_logs     -> OperationLog
--
-- 列名与 @TableField 注解、mapper XML resultMap 严格保持一致。
-- =============================================================

CREATE DATABASE IF NOT EXISTS `tengxunmeeting`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `tengxunmeeting`;

-- -------------------------------------------------------------
-- 用户表（映射实体 User，@TableName("meeting_user")）
-- 注意：状态列为 status，实体 @TableField("status")，UserMapper.xml 映射 status -> enabled
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `meeting_id` VARCHAR(50) DEFAULT NULL COMMENT '当前所在会议ID',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    KEY `idx_status` (`status`),
    KEY `idx_meeting_id` (`meeting_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------------------------
-- 会议表（映射实体 Meeting，@TableName("meeting_room")）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_room` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `meeting_number` VARCHAR(9) NOT NULL COMMENT '会议号（9位唯一数字）',
    `topic` VARCHAR(100) NOT NULL COMMENT '会议主题',
    `host_user_id` BIGINT(20) NOT NULL COMMENT '主持人用户ID',
    `room_type` VARCHAR(20) NOT NULL COMMENT '会议类型: instant/scheduled',
    `password` VARCHAR(6) DEFAULT NULL COMMENT '入会密码（可选）',
    `join_link` VARCHAR(255) DEFAULT NULL COMMENT '入会链接',
    `status` VARCHAR(20) NOT NULL COMMENT '会议状态: waiting/ongoing/ended/cancelled',
    `max_participant_count` INT(11) NOT NULL DEFAULT 300 COMMENT '最大参会人数',
    `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '预约开始时间',
    `start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
    `mute_on_entry` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '入会静音: 0-关闭, 1-开启',
    `allow_screen_share` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '允许共享屏幕: 0-禁止, 1-允许',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_meeting_number` (`meeting_number`),
    KEY `idx_host_user_id` (`host_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_scheduled_start_time` (`scheduled_start_time`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议表';

-- -------------------------------------------------------------
-- 会议参与者表（映射实体 MeetingParticipant，@TableName("meeting_participant")）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_participant` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id` BIGINT(20) NOT NULL COMMENT '会议ID',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID, 游客可为空',
    `display_name` VARCHAR(50) DEFAULT NULL COMMENT '会议内显示名',
    `role_type` VARCHAR(20) NOT NULL COMMENT '角色: host/co_host/participant/guest',
    `join_source` VARCHAR(20) DEFAULT NULL COMMENT '入会来源: app/web/mobile',
    `join_status` VARCHAR(20) NOT NULL COMMENT '状态: waiting/joined/left/kicked/rejected',
    `join_time` DATETIME DEFAULT NULL COMMENT '入会时间',
    `leave_time` DATETIME DEFAULT NULL COMMENT '离会时间',
    `leave_reason` VARCHAR(255) DEFAULT NULL COMMENT '离会原因',
    `is_microphone_on` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '麦克风是否开启: 0-关闭, 1-开启',
    `is_camera_on` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '摄像头是否开启: 0-关闭, 1-开启',
    `is_screen_sharing` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否共享屏幕: 0-否, 1-是',
    `is_hand_raised` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否举手: 0-否, 1-是',
    `network_quality` VARCHAR(20) DEFAULT NULL COMMENT '网络质量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_join_status` (`join_status`),
    KEY `idx_room_user` (`room_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参与者表';

-- -------------------------------------------------------------
-- 会议聊天消息表（映射实体 MessageSend，@TableName("meeting_chat_message")）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_chat_message` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `meeting_id` BIGINT(20) NOT NULL COMMENT '会议ID',
    `sender_id` BIGINT(20) NOT NULL COMMENT '发送者参会人ID',
    `sender_name` VARCHAR(64) DEFAULT NULL COMMENT '发送者参会人名称',
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/system/file',
    `content` TEXT COMMENT '消息内容',
    `target_scope` VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '目标范围: all/private/host',
    `receved_id` BIGINT(20) DEFAULT NULL COMMENT '私聊目标ID',
    `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `reply_to_id` BIGINT(20) DEFAULT NULL COMMENT '引用消息ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_meeting_sent_at` (`meeting_id`, `sent_at`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_reply_to_id` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议聊天消息表';

-- -------------------------------------------------------------
-- 会议语音转文字记录（映射实体 MeetingTranscript，@TableName("meeting_transcript")）
-- 注意：该实体不继承 BaseEntity，无 deleted 列；start_time/end_time 为相对会议开始的秒数
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meeting_transcript` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `meeting_id` BIGINT(20) NOT NULL COMMENT '会议ID',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '发言人用户ID',
    `user_name` VARCHAR(64) DEFAULT NULL COMMENT '发言人名称',
    `text_content` TEXT COMMENT '识别的文本内容',
    `start_time` INT(11) DEFAULT NULL COMMENT '发言开始时间（相对会议开始的秒数）',
    `end_time` INT(11) DEFAULT NULL COMMENT '发言结束时间（相对会议开始的秒数）',
    `confidence` DOUBLE DEFAULT NULL COMMENT '识别置信度 0-1',
    `is_final` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为最终结果: 0-中间结果, 1-最终结果',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_meeting_id` (`meeting_id`),
    KEY `idx_meeting_final` (`meeting_id`, `is_final`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议语音转文字记录表';

-- -------------------------------------------------------------
-- 操作日志表（映射实体 OperationLog，@TableName("fi_operation_logs")）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fi_operation_logs` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type` VARCHAR(100) NOT NULL COMMENT '操作类型（如：LOGIN、DOC_EDIT）',
    `description` TEXT COMMENT '操作描述',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '执行用户ID',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT '执行用户名',
    `operator` VARCHAR(100) DEFAULT NULL COMMENT '操作人标识（用户名或IP）',
    `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功: 0-失败, 1-成功',
    `params` TEXT COMMENT '输入参数快照',
    `result` TEXT COMMENT '输出结果快照',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================================
-- 初始化数据
-- =============================================================

-- 默认管理员账号（密码: admin123）
INSERT INTO `meeting_user` (`username`, `password`, `mobile`, `avatar`, `status`, `role`, `deleted`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', '/default-avatar.png', 1, 'admin', 0)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- 测试用户（密码: 123456）
INSERT INTO `meeting_user` (`username`, `password`, `mobile`, `avatar`, `status`, `role`, `deleted`)
VALUES ('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '13900139000', '/default-avatar.png', 1, 'user', 0)
ON DUPLICATE KEY UPDATE `username` = `username`;
