-- ============================================================
-- 实时翻译 + AI 会议总结
-- 说明：本迁移只做增量建表，不修改已有会议表结构
-- ============================================================

-- 1. 补建缺失的语音转文字表（实时翻译的事实来源）
CREATE TABLE IF NOT EXISTS meeting_transcript (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    user_id BIGINT NOT NULL COMMENT '发言人用户ID',
    user_name VARCHAR(100) NOT NULL COMMENT '发言人名称',
    text_content TEXT NOT NULL COMMENT '识别的文本内容',
    start_time INT NOT NULL COMMENT '发言开始时间（相对会议开始的秒数）',
    end_time INT NOT NULL COMMENT '发言结束时间（相对会议开始的秒数）',
    confidence DOUBLE DEFAULT 1.0 COMMENT '识别置信度 0-1',
    is_final TINYINT(1) DEFAULT 1 COMMENT '是否为最终结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_meeting_id (meeting_id),
    KEY idx_meeting_final (meeting_id, is_final),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议语音转文字记录表';

-- 2. 会议级 AI 配置
CREATE TABLE IF NOT EXISTS meeting_ai_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    translation_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启实时翻译',
    source_language VARCHAR(16) NOT NULL DEFAULT 'auto' COMMENT '源语言，auto 表示自动识别',
    target_languages JSON DEFAULT NULL COMMENT '目标语言数组，如 ["en","ja"]',
    summary_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许生成AI总结',
    summary_language VARCHAR(16) NOT NULL DEFAULT 'zh' COMMENT '总结输出语言',
    auto_summary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '会议结束后是否自动生成总结',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_meeting (meeting_id),
    CONSTRAINT fk_ai_settings_meeting FOREIGN KEY (meeting_id) REFERENCES meeting_room (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议AI配置表';

-- 3. 实时翻译记录（一句话一条，按语种拆分）
CREATE TABLE IF NOT EXISTS meeting_translation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    transcript_id BIGINT DEFAULT NULL COMMENT '对应转录记录ID',
    user_id BIGINT NOT NULL COMMENT '发言人用户ID',
    user_name VARCHAR(100) NOT NULL COMMENT '发言人名称',
    segment_index INT NOT NULL DEFAULT 0 COMMENT '会议内有序片段号',
    source_language VARCHAR(16) NOT NULL DEFAULT 'auto' COMMENT '识别出的源语言',
    target_language VARCHAR(16) NOT NULL COMMENT '目标语言',
    source_text TEXT NOT NULL COMMENT '原文',
    translated_text TEXT DEFAULT NULL COMMENT '译文',
    engine VARCHAR(32) NOT NULL DEFAULT 'llm' COMMENT '翻译引擎: llm/translate',
    latency_ms INT DEFAULT NULL COMMENT '翻译耗时（毫秒）',
    status VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT '状态: success/failed',
    is_final TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否最终结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_meeting_lang (meeting_id, target_language, id),
    KEY idx_transcript (transcript_id),
    CONSTRAINT fk_translation_meeting FOREIGN KEY (meeting_id) REFERENCES meeting_room (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议实时翻译记录表';

-- 4. AI 会议总结（按类型+版本保留多条，便于重新生成）
CREATE TABLE IF NOT EXISTS meeting_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    summary_type VARCHAR(32) NOT NULL DEFAULT 'full' COMMENT '总结类型: full/quick',
    language VARCHAR(16) NOT NULL DEFAULT 'zh' COMMENT '输出语言',
    title VARCHAR(200) DEFAULT NULL COMMENT '总结标题',
    overview TEXT DEFAULT NULL COMMENT '会议概述',
    key_points JSON DEFAULT NULL COMMENT '关键要点数组',
    decisions JSON DEFAULT NULL COMMENT '决议事项数组',
    action_items JSON DEFAULT NULL COMMENT '待办事项数组（含负责人/截止时间）',
    keywords JSON DEFAULT NULL COMMENT '关键词数组',
    model VARCHAR(64) DEFAULT NULL COMMENT '使用的模型',
    prompt_tokens INT DEFAULT NULL COMMENT '输入token数',
    completion_tokens INT DEFAULT NULL COMMENT '输出token数',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/generating/success/failed',
    trigger_type VARCHAR(16) NOT NULL DEFAULT 'auto' COMMENT '触发方式: auto/manual',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    generated_at DATETIME DEFAULT NULL COMMENT '生成完成时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_meeting_status (meeting_id, status),
    CONSTRAINT fk_summary_meeting FOREIGN KEY (meeting_id) REFERENCES meeting_room (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会议总结表';

-- 5. AI 异步任务（总结生成、翻译补算等，保证可重试、可观测）
CREATE TABLE IF NOT EXISTS meeting_ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    meeting_id BIGINT NOT NULL COMMENT '会议ID',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型: SUMMARY/TRANSLATION_BACKFILL',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    payload JSON DEFAULT NULL COMMENT '任务参数',
    result_ref_id BIGINT DEFAULT NULL COMMENT '结果ID（如总结ID）',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_meeting_type_status (meeting_id, task_type, status),
    KEY idx_status (status),
    CONSTRAINT fk_ai_task_meeting FOREIGN KEY (meeting_id) REFERENCES meeting_room (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议AI异步任务表';
