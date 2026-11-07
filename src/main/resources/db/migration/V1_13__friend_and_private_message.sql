-- ============================================================
-- 私聊与好友功能
-- 说明：
--   1. im_friend 好友关系（双向各一条，便于按用户查询与备注）
--   2. im_friend_request 好友申请（需对方同意）
--   3. im_private_message 私聊消息
-- ============================================================

CREATE TABLE IF NOT EXISTS im_friend (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    friend_id  BIGINT      NOT NULL COMMENT '好友用户ID',
    remark     VARCHAR(50) DEFAULT NULL COMMENT '好友备注',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    KEY idx_friend_id (friend_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好友关系';

CREATE TABLE IF NOT EXISTS im_friend_request (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    from_user_id  BIGINT       NOT NULL COMMENT '申请人用户ID',
    to_user_id    BIGINT       NOT NULL COMMENT '接收人用户ID',
    apply_message VARCHAR(100) DEFAULT NULL COMMENT '申请附言',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_from_to (from_user_id, to_user_id),
    KEY idx_to_status (to_user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好友申请';

CREATE TABLE IF NOT EXISTS im_private_message (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    from_user_id BIGINT        NOT NULL COMMENT '发送者用户ID',
    to_user_id   BIGINT        NOT NULL COMMENT '接收者用户ID',
    message_type VARCHAR(16)   NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT',
    content      VARCHAR(2000) NOT NULL COMMENT '消息内容',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    KEY idx_from_to (from_user_id, to_user_id, id),
    KEY idx_to_from (to_user_id, from_user_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '私聊消息';
