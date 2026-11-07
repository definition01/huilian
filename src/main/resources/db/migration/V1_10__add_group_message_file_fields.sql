-- ============================================================
-- 群聊消息支持图片/文件
-- 说明：本迁移只做增量字段，不修改已有字段
-- ============================================================

ALTER TABLE im_group_message
    ADD COLUMN file_url VARCHAR(500) DEFAULT NULL COMMENT '附件访问地址' AFTER content,
    ADD COLUMN file_name VARCHAR(255) DEFAULT NULL COMMENT '附件原始名称' AFTER file_url,
    ADD COLUMN file_size BIGINT DEFAULT NULL COMMENT '附件大小(字节)' AFTER file_name;
