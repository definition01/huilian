package cn.cxdproject.coder.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建即时会议 DTO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateInstantMeetingDTO {

    /**
     * 会议主题
     */
    @NotBlank(message = "会议主题不能为空")
    @Size(max = 100, message = "会议主题不能超过100个字符")
    private String topic;

    /**
     * 入会密码（可选，6位数字）
     */
    @Size(min = 6, max = 6, message = "密码必须是6位数字")
    private String password;

    /**
     * 最大参会人数（默认300）
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
     * 入会静音（默认false）
     */
    private Boolean muteOnEntry;

    /**
     * 允许屏幕共享（默认true）
     */
    private Boolean allowScreenShare;

    /**
     * 开启等候室（默认false）
     */
    private Boolean waitingRoomEnabled;

    /**
     * 是否录制（默认false）
     */
    private Boolean recordEnabled;

    /**
     * 自动录制（默认false）
     */
    private Boolean autoRecord;
}
