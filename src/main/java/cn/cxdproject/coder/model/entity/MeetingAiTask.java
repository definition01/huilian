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
 * 会议 AI 异步任务实体，对应 meeting_ai_task 表
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_ai_task")
public class MeetingAiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 任务类型: SUMMARY / TRANSLATION_BACKFILL
     */
    @TableField("task_type")
    private String taskType;

    /**
     * pending / running / success / failed
     */
    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("payload")
    private String payload;

    @TableField("result_ref_id")
    private Long resultRefId;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
