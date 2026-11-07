package cn.cxdproject.coder.websockt.handler;

import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.SignalingMessage;
import cn.cxdproject.coder.websockt.message.SignalingType;
import cn.cxdproject.coder.websockt.message.WsMessage;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebRTC 信令处理器
 *
 * @author heathcetide
 */
@Component
@Slf4j
public class SignalingHandler {

    @Resource
    private ChannelContextUtils channelContextUtils;

    /**
     * 处理信令消息
     */
    public void handleSignaling(WsMessage wsMessage) {
        try {
            // 解析信令消息
            SignalingMessage signalingMessage = parseSignalingMessage(wsMessage);
            if (signalingMessage == null) {
                log.warn("无法解析信令消息 - 发送者: {}", wsMessage.getSenderId());
                return;
            }

            log.info("处理信令消息 - 类型: {}, 发送者: {}, 接收者: {}, 会议: {}",
                    signalingMessage.getSignalingType(),
                    signalingMessage.getFromUserId(),
                    signalingMessage.getToUserId(),
                    signalingMessage.getMeetingId());

            // 根据不同的信令类型处理
            switch (signalingMessage.getSignalingType()) {
                case JOIN_ROOM:
                    handleJoinRoom(signalingMessage, wsMessage);
                    break;
                case OFFER:
                    handleOffer(signalingMessage, wsMessage);
                    break;
                case ANSWER:
                    handleAnswer(signalingMessage, wsMessage);
                    break;
                case ICE_CANDIDATE:
                    handleIceCandidate(signalingMessage, wsMessage);
                    break;
                case MEDIA_STATE_CHANGE:
                    handleMediaStateChange(signalingMessage, wsMessage);
                    break;
                case LEAVE_ROOM:
                    handleLeaveRoom(signalingMessage, wsMessage);
                    break;
                default:
                    log.warn("未知的信令类型: {}", signalingMessage.getSignalingType());
            }

        } catch (Exception e) {
            log.error("处理信令消息失败", e);
        }
    }

    /**
     * 处理加入房间
     */
    private void handleJoinRoom(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String meetingId = signalingMessage.getMeetingId();
        String userId = signalingMessage.getFromUserId();

        log.info("用户加入 WebRTC 房间 - 用户: {}, 会议: {}", userId, meetingId);

        // 通知房间内其他人有新用户加入
        WsMessage notification = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("GROUP")
                .meetingId(meetingId)
                .senderId(userId)
                .senderName(wsMessage.getSenderName())
                .content("USER_JOINED")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        broadcastToMeeting(meetingId, notification, userId);
    }

    /**
     * 处理 Offer（发起方创建 Offer）
     */
    private void handleOffer(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String toUserId = signalingMessage.getToUserId();
        String fromUserId = signalingMessage.getFromUserId();

        log.info("转发 Offer - 从: {} 到: {}", fromUserId, toUserId);

        // 转发 Offer 给目标用户
        WsMessage forwardMessage = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("PRIVATE")
                .receiverId(toUserId)
                .senderId(fromUserId)
                .senderName(wsMessage.getSenderName())
                .content("OFFER")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        sendToUser(toUserId, forwardMessage);
    }

    /**
     * 处理 Answer（接收方回复 Answer）
     */
    private void handleAnswer(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String toUserId = signalingMessage.getToUserId();
        String fromUserId = signalingMessage.getFromUserId();

        log.info("转发 Answer - 从: {} 到: {}", fromUserId, toUserId);

        // 转发 Answer 给目标用户
        WsMessage forwardMessage = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("PRIVATE")
                .receiverId(toUserId)
                .senderId(fromUserId)
                .senderName(wsMessage.getSenderName())
                .content("ANSWER")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        sendToUser(toUserId, forwardMessage);
    }

    /**
     * 处理 ICE Candidate
     */
    private void handleIceCandidate(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String toUserId = signalingMessage.getToUserId();
        String fromUserId = signalingMessage.getFromUserId();

        log.debug("转发 ICE Candidate - 从: {} 到: {}", fromUserId, toUserId);

        // 转发 ICE Candidate 给目标用户
        WsMessage forwardMessage = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("PRIVATE")
                .receiverId(toUserId)
                .senderId(fromUserId)
                .senderName(wsMessage.getSenderName())
                .content("ICE_CANDIDATE")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        sendToUser(toUserId, forwardMessage);
    }

    /**
     * 处理媒体状态变化（音频/视频开关）
     */
    private void handleMediaStateChange(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String meetingId = signalingMessage.getMeetingId();
        String userId = signalingMessage.getFromUserId();

        log.info("媒体状态变化 - 用户: {}, 音频: {}, 视频: {}, 共享: {}",
                userId,
                signalingMessage.getMediaState().getAudioEnabled(),
                signalingMessage.getMediaState().getVideoEnabled(),
                signalingMessage.getMediaState().getScreenSharing());

        // 广播媒体状态变化给房间内所有人
        WsMessage notification = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("GROUP")
                .meetingId(meetingId)
                .senderId(userId)
                .senderName(wsMessage.getSenderName())
                .content("MEDIA_STATE_CHANGE")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        broadcastToMeeting(meetingId, notification, null);
    }

    /**
     * 处理离开房间
     */
    private void handleLeaveRoom(SignalingMessage signalingMessage, WsMessage wsMessage) {
        String meetingId = signalingMessage.getMeetingId();
        String userId = signalingMessage.getFromUserId();

        log.info("用户离开 WebRTC 房间 - 用户: {}, 会议: {}", userId, meetingId);

        // 通知房间内其他人用户离开
        WsMessage notification = WsMessage.builder()
                .type(WsMessageType.SIGNALING)
                .targetScope("GROUP")
                .meetingId(meetingId)
                .senderId(userId)
                .senderName(wsMessage.getSenderName())
                .content("USER_LEFT")
                .data(signalingMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        broadcastToMeeting(meetingId, notification, userId);
    }

    /**
     * 解析信令消息
     */
    private SignalingMessage parseSignalingMessage(WsMessage wsMessage) {
        try {
            // 优先使用 data 字段
            if (wsMessage.getData() != null) {
                // 如果 data 已经是 SignalingMessage 对象，直接返回
                if (wsMessage.getData() instanceof SignalingMessage) {
                    return (SignalingMessage) wsMessage.getData();
                }

                // 否则从 JSON 解析
                String json = JsonUtils.toJson(wsMessage.getData());
                return JsonUtils.fromJson(json, SignalingMessage.class);
            }

            // 如果 data 为空，尝试从 content 解析
            if (wsMessage.getContent() != null) {
                return JsonUtils.fromJson(wsMessage.getContent(), SignalingMessage.class);
            }

            return null;
        } catch (Exception e) {
            log.error("解析信令消息失败", e);
            return null;
        }
    }

    /**
     * 发送消息给指定用户
     */
    private void sendToUser(String userId, WsMessage message) {
        try {
            cn.cxdproject.coder.model.dto.MessageSendDto dto = convertToMessageDto(message);
            channelContextUtils.sendMessage(dto);
        } catch (Exception e) {
            log.error("发送消息失败 - 接收者: {}", userId, e);
        }
    }

    /**
     * 广播消息给会议室所有人
     */
    private void broadcastToMeeting(String meetingId, WsMessage message, String excludeUserId) {
        try {
            cn.cxdproject.coder.model.dto.MessageSendDto dto = convertToMessageDto(message);
            if (excludeUserId != null) {
                dto.setExcludeUserId(Long.parseLong(excludeUserId));
            }
            channelContextUtils.sendMessage(dto);
        } catch (Exception e) {
            log.error("广播消息失败 - 会议: {}", meetingId, e);
        }
    }

    /**
     * 转换为旧的 MessageSendDto 格式
     */
    private cn.cxdproject.coder.model.dto.MessageSendDto convertToMessageDto(WsMessage message) {
        cn.cxdproject.coder.model.dto.MessageSendDto dto = new cn.cxdproject.coder.model.dto.MessageSendDto();

        // 设置目标范围
        if ("GROUP".equals(message.getTargetScope())) {
            dto.setTargetScope(cn.cxdproject.coder.model.MessageSend2TypeEnum.GROUP.getCode());
        } else if ("PRIVATE".equals(message.getTargetScope())) {
            dto.setTargetScope(cn.cxdproject.coder.model.MessageSend2TypeEnum.PRIVATE.getCode());
        }

        // 转换 ID
        if (message.getMeetingId() != null) {
            try {
                dto.setMeetingId(Long.parseLong(message.getMeetingId()));
            } catch (NumberFormatException e) {
                log.warn("无法解析 meetingId: {}", message.getMeetingId());
            }
        }

        if (message.getReceiverId() != null) {
            try {
                dto.setRecevedId(Long.parseLong(message.getReceiverId()));
            } catch (NumberFormatException e) {
                log.warn("无法解析 receiverId: {}", message.getReceiverId());
            }
        }

        if (message.getSenderId() != null) {
            try {
                dto.setSenderId(Long.parseLong(message.getSenderId()));
            } catch (NumberFormatException e) {
                log.warn("无法解析 senderId: {}", message.getSenderId());
            }
        }

        dto.setSenderName(message.getSenderName());
        dto.setMessageType(message.getType() != null ? message.getType().name() : null);
        dto.setContent(JsonUtils.toJson(message));

        return dto;
    }
}
