package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 会议总结实体，对应 meeting_summary 表
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_summary")
public class MeetingSummary extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 总结类型: full / quick
     */
    @TableField("summary_type")
    private String summaryType;

    @TableField("language")
    private String language;

    @TableField("title")
    private String title;

    @TableField("overview")
    private String overview;

    /**
     * 关键要点，JSON 数组字符串
     */
    @TableField("key_points")
    private String keyPoints;

    /**
     * 决议事项，JSON 数组字符串
     */
    @TableField("decisions")
    private String decisions;

    /**
     * 待办事项，JSON 数组字符串
     */
    @TableField("action_items")
    private String actionItems;

    /**
     * 关键词，JSON 数组字符串
     */
    @TableField("keywords")
    private String keywords;

    @TableField("model")
    private String model;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    /**
     * pending / generating / success / failed
     */
    @TableField("status")
    private String status;

    /**
     * auto / manual
     */
    @TableField("trigger_type")
    private String triggerType;

    @TableField("error_message")
    private String errorMessage;

    @TableField("generated_at")
    private LocalDateTime generatedAt;
}
