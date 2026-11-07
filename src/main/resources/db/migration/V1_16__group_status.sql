-- 群聊状态：normal-正常，banned-已封禁（管理员可封禁/解封）
ALTER TABLE im_group
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '群状态: normal-正常, banned-已封禁' AFTER announcement;
