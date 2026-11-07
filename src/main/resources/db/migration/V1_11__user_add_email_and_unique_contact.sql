-- ============================================================
-- 用户注册支持手机号(+86)与邮箱，并保证两者唯一
-- 说明：
--   1. 新增 email 字段
--   2. 把历史 11 位手机号统一补全为 +86 前缀，保证唯一索引可创建
--   3. 手机号、邮箱加唯一索引
-- ============================================================

ALTER TABLE meeting_user
    ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER mobile;

UPDATE meeting_user
SET mobile = CONCAT('+86', mobile)
WHERE mobile REGEXP '^1[3-9][0-9]{9}$';

ALTER TABLE meeting_user ADD UNIQUE KEY uk_user_mobile (mobile);
ALTER TABLE meeting_user ADD UNIQUE KEY uk_user_email (email);
