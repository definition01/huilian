-- 创建会议语音转文字记录表
CREATE TABLE IF NOT EXISTS meeting_transcript (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    user_id BIGINT NOT NULL COMMENT '发言人用户ID',
    user_name VARCHAR(100) NOT NULL COMMENT '发言人名称',
    text_content TEXT NOT NULL COMMENT '识别的文本内容',
    start_time INT NOT NULL COMMENT '发言开始时间（相对会议开始的秒数）',
    end_time INT NOT NULL COMMENT '发言结束时间（相对会议开始的秒数）',
    confidence DOUBLE DEFAULT 1.0 COMMENT '识别置信度 0-1',
    is_final BOOLEAN DEFAULT TRUE COMMENT '是否为最终结果',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议语音转文字记录表';
