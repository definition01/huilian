package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议详情 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingVO {

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
     * 主持人用户ID
     */
    private Long hostUserId;

    /**
     * 主持人用户名
     */
    private String hostUsername;

    /**
     * 会议纪要ID（无纪要时为空）
     */
    private Long summaryId;

    /**
     * 会议纪要状态: pending/generating/success/failed，无纪要为 null
     */
    private String summaryStatus;

    /**
     * 会议类型
     */
    private String type;

    /**
     * 是否有密码
     */
    private Boolean hasPassword;

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
     * 预约结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    /**
     * 实际开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    /**
     * 预计持续时长（分钟）
     */
    private Integer duration;

    /**
     * 最大参会人数
     */
    private Integer maxParticipants;

    /**
     * 当前参会人数
     */
    private Integer currentParticipants;

    /**
     * 入会链接
     */
    private String joinLink;

    /**
     * 会议描述
     */
    private String description;

    /**
     * 入会静音
     */
    private Boolean muteOnEntry;

    /**
     * 允许屏幕共享
     */
    private Boolean allowScreenShare;

    /**
     * 开启等候室
     */
    private Boolean waitingRoomEnabled;

    /**
     * 是否录制
     */
    private Boolean recordEnabled;

    /**
     * 自动录制
     */
    private Boolean autoRecord;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
