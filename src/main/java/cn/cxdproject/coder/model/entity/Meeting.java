package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Meeting 实体类
 * 映射到 meeting_room 表
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_room")
@EqualsAndHashCode(callSuper = true)
public class Meeting extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会议号
     */
    @TableField("meeting_number")
    private String meetingNumber;

    /**
     * 会议主题
     */
    @TableField("topic")
    private String topic;

    /**
     * 主持人用户ID
     */
    @TableField("host_user_id")
    private Long hostUserId;

    /**
     * 会议类型: instant/scheduled
     */
    @TableField("room_type")
    private String type;

    /**
     * 入会密码
     */
    @TableField("password")
    private String password;

    /**
     * 入会链接
     */
    @TableField("join_link")
    private String joinLink;

    /**
     * 会议状态: waiting/ongoing/ended/cancelled
     */
    @TableField("status")
    private String status;

    /**
     * 最大参会人数
     */
    @TableField("max_participant_count")
    private Integer maxParticipants;

    /**
     * 预约开始时间
     */
    @TableField("scheduled_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    /**
     * 预约结束时间
     */
    @TableField("scheduled_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    /**
     * 开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    /**
     * 预计持续时长（分钟）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 会议描述
     */
    @TableField("description")
    private String description;

    /**
     * 入会静音: 0-关闭, 1-开启
     */
    @TableField("mute_on_entry")
    private Boolean muteOnEntry;

    /**
     * 允许共享屏幕: 0-禁止, 1-允许
     */
    @TableField("allow_screen_share")
    private Boolean allowScreenShare;

    /**
     * 开启等候室
     */
    @TableField("waiting_room_enabled")
    private Boolean waitingRoomEnabled;

    /**
     * 是否录制
     */
    @TableField("record_enabled")
    private Boolean recordEnabled;

    /**
     * 自动录制
     */
    @TableField("auto_record")
    private Boolean autoRecord;

    @Override
    public Meeting clone() {
        try {
            return (Meeting) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone Meeting object", e);
        }
    }
}
