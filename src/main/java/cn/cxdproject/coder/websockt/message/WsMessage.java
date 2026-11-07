package cn.cxdproject.coder.websockt.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息体
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WsMessage {

    /**
     * 消息类型
     */
    private WsMessageType type;

    /**
     * 目标范围：GROUP/PRIVATE/HOST
     */
    private String targetScope;

    /**
     * 会议ID（群发时使用）
     */
    private String meetingId;

    /**
     * 接收者ID（私聊时使用）
     */
    private String receiverId;

    /**
     * 目标用户ID（主持人控制时使用）
     */
    private String targetUserId;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 控制指令（CONTROL 类型时使用）
     * 如：MUTE, UNMUTE, HAND_UP, HAND_DOWN, SCREEN_SHARE_START, SCREEN_SHARE_STOP
     */
    private String action;

    /**
     * 额外数据（JSON 格式）
     */
    private Object data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息ID（用于去重）
     */
    private String messageId;
}
