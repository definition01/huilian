-- 修复会议系统表结构缺失字段
-- 执行时间: 2026-08-04

-- ========================================
-- 1. 用户表 meeting_user
-- 代码中 User.enabled 映射到 status 字段
-- ========================================
ALTER TABLE meeting_user
    ADD COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户状态: 1-启用, 0-禁用'
    AFTER avatar;

-- ========================================
-- 2. 会议表 meeting_room
-- 以下字段之前被标记为 @TableField(exist = false)，现在改为持久化字段
-- ========================================
ALTER TABLE meeting_room
    ADD COLUMN scheduled_end_time DATETIME NULL COMMENT '预约结束时间'
        AFTER scheduled_start_time,
    ADD COLUMN duration INT NULL COMMENT '预计持续时长（分钟）'
        AFTER actual_end_time,
    ADD COLUMN description VARCHAR(500) NULL COMMENT '会议描述'
        AFTER duration,
    ADD COLUMN waiting_room_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启等候室: 0-关闭, 1-开启'
        AFTER allow_screen_share,
    ADD COLUMN record_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启录制: 0-关闭, 1-开启'
        AFTER waiting_room_enabled,
    ADD COLUMN auto_record TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动录制: 0-关闭, 1-开启'
        AFTER record_enabled;
