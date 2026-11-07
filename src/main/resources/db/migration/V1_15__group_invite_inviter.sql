-- 入群申请记录邀请人，NULL 表示用户主动通过群号申请
ALTER TABLE im_group_join_request
    ADD COLUMN inviter_id BIGINT NULL COMMENT '邀请人用户ID，NULL表示主动申请' AFTER user_id;
