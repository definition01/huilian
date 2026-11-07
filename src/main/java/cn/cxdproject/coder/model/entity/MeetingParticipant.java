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
 * MeetingParticipant 实体类
 * 会议参与者信息
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_participant")
@EqualsAndHashCode(callSuper = true)
public class MeetingParticipant extends BaseEntity implements Serializable, Cloneable {

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
    @TableField("room_id")
    private Long roomId;

    /**
     * 用户ID, 游客可为空
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会议内显示名
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 角色: host/co_host/participant/guest
     */
    @TableField("role_type")
    private String roleType;

    /**
     * 入会来源: app/web/mobile
     */
    @TableField("join_source")
    private String joinSource;

    /**
     * 状态: waiting/joined/left/kicked/rejected
     */
    @TableField("join_status")
    private String joinStatus;

    /**
     * 入会时间
     */
    @TableField("join_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    /**
     * 离会时间
     */
    @TableField("leave_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime leaveTime;

    /**
     * 离会原因
     */
    @TableField("leave_reason")
    private String leaveReason;

    /**
     * 麦克风是否开启: 0-关闭, 1-开启
     */
    @TableField("is_microphone_on")
    private Boolean isMicrophoneOn;

    /**
     * 摄像头是否开启: 0-关闭, 1-开启
     */
    @TableField("is_camera_on")
    private Boolean isCameraOn;

    /**
     * 是否共享屏幕: 0-否, 1-是
     */
    @TableField("is_screen_sharing")
    private Boolean isScreenSharing;

    /**
     * 是否举手: 0-否, 1-是
     */
    @TableField("is_hand_raised")
    private Boolean isHandRaised;

    /**
     * 网络质量
     */
    @TableField("network_quality")
    private String networkQuality;

    @Override
    public MeetingParticipant clone() {
        try {
            return (MeetingParticipant) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone MeetingParticipant object", e);
        }
    }
}
