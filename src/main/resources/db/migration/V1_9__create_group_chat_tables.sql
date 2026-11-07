-- ============================================================
-- 群聊功能
-- 说明：本迁移只做增量建表，不修改已有会议表结构
-- ============================================================

-- 1. 群聊
CREATE TABLE IF NOT EXISTS im_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_name VARCHAR(100) NOT NULL COMMENT '群名称',
    owner_id BIGINT NOT NULL COMMENT '群主用户ID',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '群头像',
    announcement VARCHAR(500) DEFAULT NULL COMMENT '群公告',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    KEY idx_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- 2. 群成员
CREATE TABLE IF NOT EXISTS im_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_type VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: owner/admin/member',
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 3. 群消息
CREATE TABLE IF NOT EXISTS im_group_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    sender_id BIGINT NOT NULL COMMENT '发送者用户ID',
    sender_name VARCHAR(100) NOT NULL COMMENT '发送者名称',
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT/SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    KEY idx_group_id_id (group_id, id),
    KEY idx_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';
