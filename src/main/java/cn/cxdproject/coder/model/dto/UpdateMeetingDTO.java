package cn.cxdproject.coder.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 更新会议 DTO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMeetingDTO {

    /**
     * 会议主题
     */
    @Size(max = 100, message = "会议主题不能超过100个字符")
    private String topic;

    /**
     * 预约开始时间（仅预约会议可修改）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    /**
     * 预计持续时长（分钟）
     */
    @Min(value = 15, message = "会议时长不能少于15分钟")
    @Max(value = 1440, message = "会议时长不能超过1440分钟（24小时）")
    private Integer duration;

    /**
     * 入会密码
     */
    @Size(min = 6, max = 6, message = "密码必须是6位数字")
    private String password;

    /**
     * 最大参会人数
     */
    @Min(value = 2, message = "最大参会人数不能少于2人")
    @Max(value = 1000, message = "最大参会人数不能超过1000人")
    private Integer maxParticipants;

    /**
     * 会议描述
     */
    @Size(max = 500, message = "会议描述不能超过500个字符")
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
}
