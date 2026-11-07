package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.mapper.ImChatStateMapper;
import cn.cxdproject.coder.mapper.ImFriendMapper;
import cn.cxdproject.coder.mapper.ImGroupMemberMapper;
import cn.cxdproject.coder.mapper.ImGroupMessageMapper;
import cn.cxdproject.coder.mapper.ImPrivateMessageMapper;
import cn.cxdproject.coder.model.entity.ImChatState;
import cn.cxdproject.coder.model.entity.ImFriend;
import cn.cxdproject.coder.model.entity.ImGroupMember;
import cn.cxdproject.coder.model.entity.ImGroupMessage;
import cn.cxdproject.coder.model.entity.ImPrivateMessage;
import cn.cxdproject.coder.service.ChatStateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 会话已读与置顶服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class ChatStateServiceImpl implements ChatStateService {

    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_PRIVATE = "PRIVATE";

    private final ImChatStateMapper chatStateMapper;
    private final ImGroupMemberMapper groupMemberMapper;
    private final ImGroupMessageMapper groupMessageMapper;
    private final ImFriendMapper friendMapper;
    private final ImPrivateMessageMapper privateMessageMapper;

    public ChatStateServiceImpl(ImChatStateMapper chatStateMapper,
                                ImGroupMemberMapper groupMemberMapper,
                                ImGroupMessageMapper groupMessageMapper,
                                ImFriendMapper friendMapper,
                                ImPrivateMessageMapper privateMessageMapper) {
        this.chatStateMapper = chatStateMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.friendMapper = friendMapper;
        this.privateMessageMapper = privateMessageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, String type, Long targetId) {
        String conversationType = normalizeType(type);
        requireConversation(userId, conversationType, targetId);

        ImChatState state = findState(userId, conversationType, targetId);
        Long lastReadMessageId = latestMessageId(userId, conversationType, targetId);
        if (state == null) {
            state = ImChatState.builder()
                    .userId(userId)
                    .conversationType(conversationType)
                    .targetId(targetId)
                    .lastReadMessageId(lastReadMessageId)
                    .pinned(false)
                    .build();
            state.setCreatedAt(LocalDateTime.now());
            chatStateMapper.insert(state);
            return;
        }
        if (!Objects.equals(state.getLastReadMessageId(), lastReadMessageId)) {
            state.setLastReadMessageId(lastReadMessageId);
            state.setUpdatedAt(LocalDateTime.now());
            chatStateMapper.updateById(state);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPinned(Long userId, String type, Long targetId, Boolean pinned) {
        String conversationType = normalizeType(type);
        requireConversation(userId, conversationType, targetId);

        boolean value = Boolean.TRUE.equals(pinned);
        ImChatState state = findState(userId, conversationType, targetId);
        if (state == null) {
            state = ImChatState.builder()
                    .userId(userId)
                    .conversationType(conversationType)
                    .targetId(targetId)
                    .lastReadMessageId(0L)
                    .pinned(value)
                    .build();
            state.setCreatedAt(LocalDateTime.now());
            chatStateMapper.insert(state);
            return;
        }
        state.setPinned(value);
        state.setUpdatedAt(LocalDateTime.now());
        chatStateMapper.updateById(state);
    }

    @Override
    public List<ImChatState> listStates(Long userId, String type) {
        String conversationType = normalizeType(type);
        return chatStateMapper.selectList(new QueryWrapper<ImChatState>()
                .eq("user_id", userId)
                .eq("conversation_type", conversationType)
                .eq("deleted", false));
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        if (!TYPE_GROUP.equals(value) && !TYPE_PRIVATE.equals(value)) {
            throw new BusinessException(400, "会话类型只能是 GROUP 或 PRIVATE");
        }
        return value;
    }

    /**
     * 校验会话归属：群聊需是群成员，私聊需是好友
     */
    private void requireConversation(Long userId, String type, Long targetId) {
        if (targetId == null) {
            throw new BusinessException(400, "会话目标ID不能为空");
        }
        if (TYPE_GROUP.equals(type)) {
            long count = groupMemberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                    .eq("group_id", targetId)
                    .eq("user_id", userId)
                    .eq("deleted", false));
            if (count == 0) {
                throw new BusinessException(400, "你不在该群聊中");
            }
            return;
        }
        long count = friendMapper.selectCount(new QueryWrapper<ImFriend>()
                .eq("user_id", userId)
                .eq("friend_id", targetId)
                .eq("deleted", false));
        if (count == 0) {
            throw new BusinessException(400, "对方还不是你的好友");
        }
    }

    /**
     * 查询会话当前最新的消息ID
     */
    private Long latestMessageId(Long userId, String type, Long targetId) {
        if (TYPE_GROUP.equals(type)) {
            ImGroupMessage message = groupMessageMapper.selectOne(new QueryWrapper<ImGroupMessage>()
                    .select("id")
                    .eq("group_id", targetId)
                    .eq("deleted", false)
                    .orderByDesc("id")
                    .last("LIMIT 1"));
            return message == null ? 0L : message.getId();
        }
        ImPrivateMessage message = privateMessageMapper.selectOne(new QueryWrapper<ImPrivateMessage>()
                .select("id")
                .eq("deleted", false)
                .and(w -> w
                        .and(x -> x.eq("from_user_id", userId).eq("to_user_id", targetId))
                        .or(x -> x.eq("from_user_id", targetId).eq("to_user_id", userId)))
                .orderByDesc("id")
                .last("LIMIT 1"));
        return message == null ? 0L : message.getId();
    }

    private ImChatState findState(Long userId, String type, Long targetId) {
        return chatStateMapper.selectOne(new QueryWrapper<ImChatState>()
                .eq("user_id", userId)
                .eq("conversation_type", type)
                .eq("target_id", targetId)
                .eq("deleted", false));
    }
}
