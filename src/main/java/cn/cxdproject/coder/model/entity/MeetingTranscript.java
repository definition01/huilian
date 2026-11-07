package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会议语音转文字记录
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_transcript")
public class MeetingTranscript implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会议ID
     */
    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 发言人用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 发言人名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 识别的文本内容
     */
    @TableField("text_content")
    private String textContent;

    /**
     * 发言开始时间（相对会议开始的秒数）
     */
    @TableField("start_time")
    private Integer startTime;

    /**
     * 发言结束时间（相对会议开始的秒数）
     */
    @TableField("end_time")
    private Integer endTime;

    /**
     * 识别置信度 0-1
     */
    @TableField("confidence")
    private Double confidence;

    /**
     * 是否为最终结果（false为中间结果）
     */
    @TableField("is_final")
    private Boolean isFinal;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
