-- ============================================================
-- 会话未读与置顶状态
-- 说明：
--   1. 每个用户对每个会话（群聊 GROUP / 私聊 PRIVATE）保存已读到的消息 ID 与置顶状态
--   2. 未读数量由服务端按 last_read_message_id 计算
-- ============================================================

CREATE TABLE IF NOT EXISTS im_chat_state (
    id                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id              BIGINT      NOT NULL COMMENT '用户ID',
    conversation_type    VARCHAR(16) NOT NULL COMMENT '会话类型: GROUP/PRIVATE',
    target_id            BIGINT      NOT NULL COMMENT '会话目标ID（群ID或好友用户ID）',
    last_read_message_id BIGINT      NOT NULL DEFAULT 0 COMMENT '已读到的最后一条消息ID',
    pinned               TINYINT     NOT NULL DEFAULT 0 COMMENT '是否置顶: 0否 1是',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_conv (user_id, conversation_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话已读与置顶状态';
