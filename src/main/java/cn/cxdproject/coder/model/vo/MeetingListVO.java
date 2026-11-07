package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议列表项 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingListVO {

    /**
     * 会议ID
     */
    private Long id;

    /**
     * 会议号
     */
    private String meetingNumber;

    /**
     * 会议主题
     */
    private String topic;

    /**
     * 创建者（主持人）用户ID
     */
    private Long hostUserId;

    /**
     * 主持人用户名
     */
    private String hostUsername;

    /**
     * 会议类型
     */
    private String type;

    /**
     * 会议状态
     */
    private String status;

    /**
     * 预约开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    /**
     * 实际开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;

    /**
     * 预计持续时长（分钟）
     */
    private Integer duration;

    /**
     * 当前参会人数
     */
    private Integer currentParticipants;

    /**
     * 最大参会人数
     */
    private Integer maxParticipants;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
