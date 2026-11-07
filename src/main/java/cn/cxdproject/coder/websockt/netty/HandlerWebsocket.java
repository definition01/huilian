package cn.cxdproject.coder.websockt.netty;

import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.handler.SignalingHandler;
import cn.cxdproject.coder.websockt.message.WsMessage;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebSocket 消息处理器
 * 处理会议相关的消息
 *
 * @author heathcetide
 */
@Component
@ChannelHandler.Sharable
@Slf4j
public class HandlerWebsocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SignalingHandler signalingHandler;

    @Resource
    private cn.cxdproject.coder.service.VolcengineAsrService volcengineAsrService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新的 WebSocket 连接建立 - Channel ID: {}", ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取用户ID
        String userId = getUserId(ctx);
        if (userId == null) {
            log.info("WebSocket 连接断开 - 未认证用户, Channel ID: {}", ctx.channel().id());
            return;
        }

        log.info("WebSocket 连接断开 - 用户ID: {}, Channel ID: {}", userId, ctx.channel().id());

        // 群聊等非会议通道断开时不触发离开会议逻辑，避免误清用户会议状态
        if (Boolean.TRUE.equals(ctx.channel().attr(ChannelContextUtils.CHAT_ONLY_KEY).get())) {
            channelContextUtils.closeContext(userId, ctx.channel());
            return;
        }

        // 同一用户可能有多个连接（重连/多标签页）。只有当前生效的连接断开才需要
        // 广播离开、清理数据库状态；旧连接断开不能误删新连接
        if (!channelContextUtils.isCurrentChannel(userId, ctx.channel())) {
            log.info("旧连接断开，跳过离开通知 - 用户ID: {}, Channel ID: {}", userId, ctx.channel().id());
            channelContextUtils.closeContext(userId, ctx.channel());
            return;
        }

        try {
            Long userIdLong = Long.parseLong(userId);
            User user = userMapper.selectById(userIdLong);

            // 优先使用握手时记录的会议ID：主持人创建即时会议后并不一定在数据库里落 meetingId
            String meetingId = ctx.channel().attr(ChannelContextUtils.MEETING_ID_KEY).get();
            if ((meetingId == null || meetingId.isEmpty()) && user != null) {
                meetingId = user.getMeetingId();
            }

            if (meetingId != null && !meetingId.isEmpty()) {
                String username = user != null ? user.getUsername() : userId;

                // 从会议房间移除用户
                channelContextUtils.removeFromMeetingRoom(meetingId, userId, ctx.channel());

                // 发送系统消息通知其他人
                sendSystemMessage(meetingId, userId, username + " 离开了会议");
                // 广播控制消息，让客户端刷新参会人列表并清理对应的 PeerConnection
                sendControlMessage(meetingId, userId, username, "USER_LEFT");

                // 清除数据库中的 meetingId
                if (user != null && user.getMeetingId() != null) {
                    user.setMeetingId(null);
                    userMapper.updateById(user);
                }

                log.info("用户离开会议房间 - 用户ID: {}, 会议ID: {}", userId, meetingId);
            }

            // 清理用户连接
            channelContextUtils.closeContext(userId, ctx.channel());

        } catch (Exception e) {
            log.error("处理用户断开连接失败 - 用户ID: {}", userId, e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        String text = frame.text();

        // 获取用户ID
        String userId = getUserId(ctx);
        if (userId == null) {
            log.warn("收到未认证用户的消息");
            sendErrorMessage(ctx, "用户未认证");
            return;
        }

        try {
            // 解析消息
            WsMessage message = JsonUtils.fromJson(text, WsMessage.class);
            if (message == null) {
                log.warn("无法解析消息 - 用户ID: {}, 消息: {}", userId, text);
                sendErrorMessage(ctx, "消息格式错误");
                return;
            }

            // 设置发送者信息
            message.setSenderId(userId);
            message.setTimestamp(System.currentTimeMillis());

            // 获取用户名
            try {
                User user = userMapper.selectById(Long.parseLong(userId));
                if (user != null) {
                    message.setSenderName(user.getUsername());
                }
            } catch (Exception e) {
                log.error("获取用户名失败 - 用户ID: {}", userId, e);
            }

            // 音频帧按 64ms 上报，用 debug 输出避免日志刷屏
            if (message.getType() == WsMessageType.AUDIO) {
                log.debug("收到音频帧 - 用户ID: {}", userId);
            } else {
                log.info("收到消息 - 用户ID: {}, 类型: {}, 目标: {}",
                        userId, message.getType(), message.getTargetScope());
            }

            // 处理不同类型的消息
            handleMessage(ctx, message);

        } catch (Exception e) {
            log.error("处理消息失败 - 用户ID: {}, 消息: {}", userId, text, e);
            sendErrorMessage(ctx, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理消息
     */
    private void handleMessage(ChannelHandlerContext ctx, WsMessage message) {
        switch (message.getType()) {
            case TEXT:
                handleTextMessage(message);
                break;
            case CONTROL:
                handleControlMessage(message);
                break;
            case SIGNALING:
                handleSignalingMessage(message);
                break;
            case AUDIO:
                handleAudioMessage(message);
                break;
            case HEARTBEAT:
                handleHeartbeatMessage(ctx, message);
                break;
            default:
                log.warn("未知的消息类型: {}", message.getType());
        }
    }

    /**
     * 处理文本消息（聊天）
     */
    private void handleTextMessage(WsMessage message) {
        log.debug("处理文本消息 - 发送者: {}, 内容: {}", message.getSenderId(), message.getContent());

        // 广播消息
        channelContextUtils.sendMessage(convertToMessageDto(message));
    }

    /**
     * 处理控制消息（静音、举手等）
     */
    private void handleControlMessage(WsMessage message) {
        log.info("处理控制消息 - 发送者: {}, 动作: {}", message.getSenderId(), message.getAction());

        // 根据不同的控制动作处理
        switch (message.getAction()) {
            case "MUTE":
                log.info("用户请求静音 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "UNMUTE":
                log.info("用户取消静音 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "MUTE_PARTICIPANT":
                log.info("主持人静音参与者 - 主持人: {}, 目标: {}", message.getSenderId(), message.getTargetUserId());
                // 只发送给目标用户
                if (message.getTargetUserId() != null) {
                    MessageSendDto targetMsg = convertToMessageDto(message);
                    targetMsg.setRecevedId(Long.parseLong(message.getTargetUserId()));
                    targetMsg.setTargetScope("PRIVATE");
                    channelContextUtils.sendMessage(targetMsg);
                }
                break;
            case "UNMUTE_PARTICIPANT":
                log.info("主持人取消静音参与者 - 主持人: {}, 目标: {}", message.getSenderId(), message.getTargetUserId());
                // 只发送给目标用户
                if (message.getTargetUserId() != null) {
                    MessageSendDto targetMsg = convertToMessageDto(message);
                    targetMsg.setRecevedId(Long.parseLong(message.getTargetUserId()));
                    targetMsg.setTargetScope("PRIVATE");
                    channelContextUtils.sendMessage(targetMsg);
                }
                break;
            case "REMOVE_PARTICIPANT":
                log.info("主持人移除参与者 - 主持人: {}, 目标: {}", message.getSenderId(), message.getTargetUserId());
                if (message.getTargetUserId() != null) {
                    // 发送给目标用户，通知其被移除
                    MessageSendDto targetMsg = convertToMessageDto(message);
                    targetMsg.setRecevedId(Long.parseLong(message.getTargetUserId()));
                    targetMsg.setTargetScope("PRIVATE");
                    channelContextUtils.sendMessage(targetMsg);
                    // 广播给其他人（排除被移除者），通知有人被移除
                    MessageSendDto groupMsg = convertToMessageDto(message);
                    groupMsg.setExcludeUserId(Long.parseLong(message.getTargetUserId()));
                    channelContextUtils.sendMessage(groupMsg);
                }
                break;
            case "MUTE_ALL":
                log.info("主持人全体静音 - 主持人: {}", message.getSenderId());
                // 广播给所有人
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "HAND_UP":
                log.info("用户举手 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "HAND_DOWN":
                log.info("用户放下手 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "SCREEN_SHARE_START":
                log.info("用户开始屏幕共享 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            case "SCREEN_SHARE_STOP":
                log.info("用户停止屏幕共享 - 用户ID: {}", message.getSenderId());
                // 广播控制消息
                channelContextUtils.sendMessage(convertToMessageDto(message));
                break;
            default:
                log.warn("未知的控制动作: {}", message.getAction());
        }
    }

    /**
     * 处理信令消息（WebRTC SDP/ICE）
     */
    private void handleSignalingMessage(WsMessage message) {
        log.debug("处理信令消息 - 发送者: {}, 内容: {}", message.getSenderId(), message.getContent());

        // 委托给信令处理器
        signalingHandler.handleSignaling(message);
    }

    /**
     * 处理音频消息（ASR音频数据）
     */
    private void handleAudioMessage(WsMessage message) {
        log.debug("收到音频数据 - 用户ID: {}, 会议ID: {}", message.getSenderId(), message.getMeetingId());

        try {
            // 将 Base64 音频数据解码为字节数组
            String audioBase64 = message.getContent();
            if (audioBase64 == null || audioBase64.isEmpty()) {
                log.warn("音频数据为空 - 用户ID: {}", message.getSenderId());
                return;
            }

            byte[] audioData = java.util.Base64.getDecoder().decode(audioBase64);

            // 解析 meetingId 和 userId
            Long meetingId = null;
            Long userId = null;

            if (message.getMeetingId() != null) {
                try {
                    meetingId = Long.parseLong(message.getMeetingId());
                } catch (NumberFormatException e) {
                    log.warn("无法解析 meetingId: {}", message.getMeetingId());
                }
            }

            if (message.getSenderId() != null) {
                try {
                    userId = Long.parseLong(message.getSenderId());
                } catch (NumberFormatException e) {
                    log.warn("无法解析 userId: {}", message.getSenderId());
                }
            }

            if (meetingId != null && userId != null) {
                // 发送音频数据到 ASR 服务
                volcengineAsrService.sendAudio(meetingId, userId, audioData);
            } else {
                log.warn("meetingId 或 userId 为空，无法处理音频数据");
            }

        } catch (Exception e) {
            log.error("处理音频消息失败 - 用户ID: {}", message.getSenderId(), e);
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(ChannelHandlerContext ctx, WsMessage message) {
        log.debug("收到心跳 - 用户ID: {}", message.getSenderId());

        // 回复心跳
        WsMessage pong = WsMessage.builder()
                .type(WsMessageType.HEARTBEAT)
                .content("pong")
                .timestamp(System.currentTimeMillis())
                .build();

        ctx.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(pong)));
    }

    /**
     * 发送系统消息
     */
    private void sendSystemMessage(String meetingId, String userId, String content) {
        try {
            WsMessage systemMessage = WsMessage.builder()
                    .type(WsMessageType.SYSTEM)
                    .targetScope("GROUP")
                    .meetingId(meetingId)
                    .senderId("SYSTEM")
                    .senderName("系统")
                    .content(content)
                    .timestamp(System.currentTimeMillis())
                    .build();

            channelContextUtils.sendMessage(convertToMessageDto(systemMessage));
        } catch (Exception e) {
            log.error("发送系统消息失败", e);
        }
    }

    /**
     * 向会议房间广播控制消息
     */
    private void sendControlMessage(String meetingId, String userId, String senderName, String action) {
        try {
            WsMessage controlMessage = WsMessage.builder()
                    .type(WsMessageType.CONTROL)
                    .targetScope("GROUP")
                    .meetingId(meetingId)
                    .senderId(userId)
                    .senderName(senderName)
                    .action(action)
                    .timestamp(System.currentTimeMillis())
                    .build();

            channelContextUtils.sendMessage(convertToMessageDto(controlMessage));
        } catch (Exception e) {
            log.error("发送控制消息失败 - 动作: {}", action, e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String errorMsg) {
        WsMessage error = WsMessage.builder()
                .type(WsMessageType.ERROR)
                .content(errorMsg)
                .timestamp(System.currentTimeMillis())
                .build();

        ctx.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(error)));
    }

    /**
     * 获取用户ID
     */
    private String getUserId(ChannelHandlerContext ctx) {
        try {
            AttributeKey<Long> attributeKey = AttributeKey.valueOf("userId");
            Long userId = ctx.channel().attr(attributeKey).get();
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 转换为旧的 MessageSendDto 格式（兼容现有代码）
     */
    private cn.cxdproject.coder.model.dto.MessageSendDto convertToMessageDto(WsMessage message) {
        cn.cxdproject.coder.model.dto.MessageSendDto dto = new cn.cxdproject.coder.model.dto.MessageSendDto();

        // 设置目标范围（使用枚举的 code）
        if ("GROUP".equals(message.getTargetScope())) {
            dto.setTargetScope(cn.cxdproject.coder.model.MessageSend2TypeEnum.GROUP.getCode());
        } else if ("PRIVATE".equals(message.getTargetScope())) {
            dto.setTargetScope(cn.cxdproject.coder.model.MessageSend2TypeEnum.PRIVATE.getCode());
        } else if ("HOST".equals(message.getTargetScope())) {
            dto.setTargetScope(cn.cxdproject.coder.model.MessageSend2TypeEnum.HOST.getCode());
        }

        // 转换 meetingId 和 receiverId 为 Long
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

        // 设置发送者信息
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String userId = getUserId(ctx);
        log.error("WebSocket 处理异常 - 用户ID: {}, Channel ID: {}",
                userId, ctx.channel().id(), cause);

        // 发送错误消息
        sendErrorMessage(ctx, "服务器内部错误");

        // 不关闭连接，让客户端决定是否重连
    }
}
