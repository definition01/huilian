package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 实时翻译记录实体，对应 meeting_translation 表
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_translation")
public class MeetingTranslation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 对应的转录记录ID
     */
    @TableField("transcript_id")
    private Long transcriptId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    /**
     * 会议内片段序号，用于前端排序
     */
    @TableField("segment_index")
    private Integer segmentIndex;

    @TableField("source_language")
    private String sourceLanguage;

    @TableField("target_language")
    private String targetLanguage;

    @TableField("source_text")
    private String sourceText;

    @TableField("translated_text")
    private String translatedText;

    /**
     * 翻译引擎标识
     */
    @TableField("engine")
    private String engine;

    /**
     * 翻译耗时（毫秒）
     */
    @TableField("latency_ms")
    private Integer latencyMs;

    /**
     * success / failed
     */
    @TableField("status")
    private String status;

    @TableField("is_final")
    private Boolean isFinal;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
