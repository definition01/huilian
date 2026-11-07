package cn.cxdproject.coder.websockt;

import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.service.UserService;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.utils.RedisUtils;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChannelContextUtils {

    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");

    /**
     * 连接所属会议ID，断开时用于清理房间
     */
    public static final AttributeKey<String> MEETING_ID_KEY = AttributeKey.valueOf("meetingId");

    /**
     * 仅消息通道（群聊等非会议场景），断开时不触发离开会议逻辑
     */
    public static final AttributeKey<Boolean> CHAT_ONLY_KEY = AttributeKey.valueOf("chatOnly");

    public static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, ChannelGroup> MEETING_ROOM = new ConcurrentHashMap<>();

    @Resource
    private RedisUtils redisUtils;
    @Resource
    private UserService userService;

    public void addContext(Long userId, Channel channel){
        addContext(userId, channel, null);
    }

    /**
     * 初始化连接并加入会议房间
     *
     * @param meetingId 握手 URL 上的会议ID，优先级高于数据库中的 meetingId；
     *                  不能使用 JWT 里的 meetingId，token 是登录时签发的，值会过期
     */
    public void addContext(Long userId, Channel channel, String meetingId){
        try {
            bindUser(userId, channel);

            String roomId = meetingId;
            if (roomId == null || roomId.isEmpty()) {
                User dbUser = userService.getById(userId);
                roomId = dbUser != null ? dbUser.getMeetingId() : null;
            }

            if (roomId != null && !roomId.isEmpty()) {
                channel.attr(MEETING_ID_KEY).set(roomId);
                addMeetingRoom(roomId, userId.toString());
                log.info("用户加入会议房间 - 用户ID: {}, 会议ID: {}", userId, roomId);
            }

        }catch (Exception e){
            log.error("初始化连接失败",e);
        }
    }

    /**
     * 群聊等非会议场景：只建立用户消息通道，不加入会议房间
     */
    public void addChatContext(Long userId, Channel channel){
        try {
            bindUser(userId, channel);
            channel.attr(CHAT_ONLY_KEY).set(true);
            log.info("群聊消息通道已建立 - 用户ID: {}", userId);
        }catch (Exception e){
            log.error("初始化群聊连接失败",e);
        }
    }

    /**
     * 记录用户连接，处理同账号重复连接的情况
     */
    private void bindUser(Long userId, Channel channel){
        channel.attr(USER_ID_KEY).set(userId);

        // 同一用户重复连接（多标签页/重连）时，关闭并移除旧连接，
        // 否则旧连接断开时会把新连接从 USER_CONTEXT_MAP 中删掉，导致再也收不到消息
        Channel oldChannel = USER_CONTEXT_MAP.put(userId.toString(), channel);
        if (oldChannel != null && !oldChannel.id().equals(channel.id())) {
            removeChannelFromRooms(oldChannel);
            // 4001 表示“连接已被新连接替换”，前端据此不再自动重连，避免两个标签页互相踢下线
            oldChannel.writeAndFlush(new CloseWebSocketFrame(4001, "replaced by new connection"))
                    .addListener(ChannelFutureListener.CLOSE);
            log.info("用户存在旧连接，已关闭旧连接 - 用户ID: {}, 旧Channel: {}", userId, oldChannel.id());
        }

        userService.updateLastLogTime(userId);
    }

    public void addMeetingRoom(String meetingId,String userId){
        Channel context = USER_CONTEXT_MAP.get(userId);
        if(context == null){
            return;
        }
        ChannelGroup group = MEETING_ROOM.get(meetingId);
        if (group == null){
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            MEETING_ROOM.put(meetingId,group);
        }
        Channel channel = group.find(context.id());
        if (channel == null){
            group.add(context);
        }
    }

    public void sendMessage(MessageSendDto messageSendDto){
        // 比较字符串而不是枚举对象
        String targetScope = messageSendDto.getTargetScope();
        if (targetScope == null) {
            log.warn("消息目标范围为空");
            return;
        }

        if (MessageSend2TypeEnum.PRIVATE.getCode().equals(targetScope)){
            sendToUser(messageSendDto);
        } else if (MessageSend2TypeEnum.GROUP.getCode().equals(targetScope)) {
            sendToGroup(messageSendDto);
        } else if (MessageSend2TypeEnum.HOST.getCode().equals(targetScope)) {
            sendToHost(messageSendDto);
        } else {
            log.warn("未知的消息目标范围: {}", targetScope);
        }
    }

    private void sendToUser(MessageSendDto messageSendDto){
       if (messageSendDto.getRecevedId() == null){
           return;
       }
       // 修复：使用 String 作为键
       Channel channel = USER_CONTEXT_MAP.get(messageSendDto.getRecevedId().toString());
       if (channel == null){
           log.debug("用户不在线，无法发送消息 - 用户ID: {}", messageSendDto.getRecevedId());
           return;
       }
       channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(messageSendDto)));
    }

    private void sendToGroup(MessageSendDto messageSendDto){
         if (messageSendDto.getMeetingId() == null){
             return;
         }
         ChannelGroup group = MEETING_ROOM.get(messageSendDto.getMeetingId().toString());
         if (group == null){
             log.debug("会议房间不存在 - 会议ID: {}", messageSendDto.getMeetingId());
             return;
         }
         Long excludeUserId = messageSendDto.getExcludeUserId();
         Channel excludeChannel = excludeUserId != null ? USER_CONTEXT_MAP.get(excludeUserId.toString()) : null;
         String json = JsonUtils.toJson(messageSendDto);
         TextWebSocketFrame frame = new TextWebSocketFrame(json);
         if (excludeChannel != null) {
             for (Channel ch : group) {
                 if (!ch.id().equals(excludeChannel.id())) {
                     ch.writeAndFlush(frame.retainedDuplicate());
                 }
             }
             frame.release();
         } else {
             group.writeAndFlush(frame);
         }
         log.debug("群发消息成功 - 会议ID: {}, 接收人数: {}", messageSendDto.getMeetingId(), group.size());
    }

    private void sendToHost(MessageSendDto messageSendDto){
        if (messageSendDto.getMeetingId() == null) {
            return;
        }
        Long hostId = (Long)redisUtils.get("meeting:host:" + messageSendDto.getMeetingId());
        if (hostId == null){
            log.debug("未找到会议主持人 - 会议ID: {}", messageSendDto.getMeetingId());
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(hostId.toString());
        if (channel == null){
            log.debug("主持人不在线 - 主持人ID: {}", hostId);
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(messageSendDto)));
    }

    public boolean isCurrentChannel(String userId, Channel channel) {
        if (userId == null || channel == null) {
            return false;
        }
        Channel current = USER_CONTEXT_MAP.get(userId);
        return current != null && current.id().equals(channel.id());
    }

    private void removeChannelFromRooms(Channel channel) {
        if (channel == null) {
            return;
        }
        for (ChannelGroup group : MEETING_ROOM.values()) {
            group.remove(channel);
        }
    }

    /**
     * 关闭连接。只有关闭的确实是当前连接时才清理映射，
     * 避免旧连接断开时误删新连接
     */
    public void closeContext(String userId, Channel channel) {
        if (userId == null || channel == null) {
            return;
        }
        Channel current = USER_CONTEXT_MAP.get(userId);
        if (current != null && current.id().equals(channel.id())) {
            USER_CONTEXT_MAP.remove(userId);
        }
        removeChannelFromRooms(channel);
        channel.close();
    }

    /**
     * 关闭用户当前连接（兼容旧调用）
     */
    public void closeContext(String userId) {
        if (userId == null) {
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel != null) {
            closeContext(userId, channel);
        }
    }

    /**
     * 清空指定会议房间
     *
     * @param meetingId 会议ID
     */
    public void clearMeetingRoom(String meetingId) {
        if (meetingId == null) {
            return;
        }
        ChannelGroup group = MEETING_ROOM.remove(meetingId);
        if (group != null) {
            log.info("会议房间已清空 - 会议ID: {}, 原有人数: {}", meetingId, group.size());
        }
    }

    /**
     * 从会议房间移除用户
     *
     * @param meetingId 会议ID
     * @param userId    用户ID
     */
    public void removeFromMeetingRoom(String meetingId, String userId) {
        removeFromMeetingRoom(meetingId, userId, USER_CONTEXT_MAP.get(userId));
    }

    /**
     * 从会议房间移除指定连接（而不是该用户当前的连接）
     */
    public void removeFromMeetingRoom(String meetingId, String userId, Channel channel) {
        if (meetingId == null || userId == null) {
            return;
        }

        ChannelGroup group = MEETING_ROOM.get(meetingId);
        if (group == null) {
            return;
        }

        if (channel != null) {
            group.remove(channel);
            log.info("用户已从会议房间移除 - 会议ID: {}, 用户ID: {}, 剩余人数: {}",
                    meetingId, userId, group.size());
        }

        // 如果房间为空，删除房间
        if (group.isEmpty()) {
            MEETING_ROOM.remove(meetingId);
            log.info("会议房间已清空并删除 - 会议ID: {}", meetingId);
        }
    }

    /**
     * 获取会议房间当前人数
     *
     * @param meetingId 会议ID
     * @return 当前人数
     */
    public int getMeetingRoomSize(String meetingId) {
        if (meetingId == null) {
            return 0;
        }
        ChannelGroup group = MEETING_ROOM.get(meetingId);
        return group != null ? group.size() : 0;
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(String userId) {
        if (userId == null) {
            return false;
        }
        Channel channel = USER_CONTEXT_MAP.get(userId);
        return channel != null && channel.isActive();
    }

}
