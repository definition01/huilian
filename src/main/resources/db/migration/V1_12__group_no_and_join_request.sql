-- ============================================================
-- 群聊支持 10 位数字群号 + 群号搜索入群审核
-- 说明：
--   1. im_group 新增 group_no（10 位数字，唯一）
--   2. 历史群按 id 生成 9 开头的 10 位群号
--   3. 新增入群申请表 im_group_join_request
-- ============================================================

ALTER TABLE im_group
    ADD COLUMN group_no VARCHAR(10) DEFAULT NULL COMMENT '群号(10位数字)' AFTER group_name;

UPDATE im_group
SET group_no = CONCAT('9', LPAD(id, 9, '0'))
WHERE group_no IS NULL;

ALTER TABLE im_group ADD UNIQUE KEY uk_group_no (group_no);

CREATE TABLE IF NOT EXISTS im_group_join_request (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    group_id   BIGINT       NOT NULL COMMENT '群ID',
    user_id    BIGINT       NOT NULL COMMENT '申请人用户ID',
    status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_group_status (group_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '群聊入群申请';
