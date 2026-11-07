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
 * 会议 AI 配置实体，对应 meeting_ai_settings 表
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_ai_settings")
public class MeetingAiSettings {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 是否开启实时翻译
     */
    @TableField("translation_enabled")
    private Boolean translationEnabled;

    /**
     * 源语言，auto 表示自动识别
     */
    @TableField("source_language")
    private String sourceLanguage;

    /**
     * 目标语言列表，JSON 数组字符串
     */
    @TableField("target_languages")
    private String targetLanguages;

    /**
     * 是否允许生成 AI 总结
     */
    @TableField("summary_enabled")
    private Boolean summaryEnabled;

    /**
     * 总结输出语言
     */
    @TableField("summary_language")
    private String summaryLanguage;

    /**
     * 会议结束后是否自动生成总结
     */
    @TableField("auto_summary")
    private Boolean autoSummary;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
