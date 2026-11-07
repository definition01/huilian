package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.ImChatStateMapper;
import cn.cxdproject.coder.mapper.ImFriendMapper;
import cn.cxdproject.coder.mapper.ImFriendRequestMapper;
import cn.cxdproject.coder.mapper.ImPrivateMessageMapper;
import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.dto.SendFriendRequestDTO;
import cn.cxdproject.coder.model.dto.SendPrivateMessageDTO;
import cn.cxdproject.coder.model.entity.ImChatState;
import cn.cxdproject.coder.model.entity.ImFriend;
import cn.cxdproject.coder.model.entity.ImFriendRequest;
import cn.cxdproject.coder.model.entity.ImPrivateMessage;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.FriendRequestVO;
import cn.cxdproject.coder.model.vo.FriendSearchVO;
import cn.cxdproject.coder.model.vo.FriendVO;
import cn.cxdproject.coder.model.vo.PrivateMessageVO;
import cn.cxdproject.coder.service.FriendService;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 好友与私聊服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class FriendServiceImpl implements FriendService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String RELATION_NONE = "NONE";
    private static final String RELATION_FRIEND = "FRIEND";
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";

    private static final String TYPE_TEXT = "TEXT";

    /**
     * 中国手机号（可带 +86 前缀）
     */
    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^(?:\\+86)?1[3-9]\\d{9}$");

    private final ImFriendMapper friendMapper;
    private final ImFriendRequestMapper requestMapper;
    private final ImPrivateMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChannelContextUtils channelContextUtils;
    private final ImChatStateMapper chatStateMapper;

    public FriendServiceImpl(ImFriendMapper friendMapper,
                             ImFriendRequestMapper requestMapper,
                             ImPrivateMessageMapper messageMapper,
                             UserMapper userMapper,
                             ChannelContextUtils channelContextUtils,
                             ImChatStateMapper chatStateMapper) {
        this.friendMapper = friendMapper;
        this.requestMapper = requestMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.channelContextUtils = channelContextUtils;
        this.chatStateMapper = chatStateMapper;
    }

    @Override
    public List<FriendSearchVO> searchUsers(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(400, "请输入手机号或用户名");
        }

        String value = keyword.trim();
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        wrapper.ne("id", userId);
        if (CHINA_MOBILE_PATTERN.matcher(value).matches()) {
            wrapper.eq("mobile", normalizeMobile(value));
        } else {
            // 用户名不唯一，模糊匹配的结果全部返回
            wrapper.like("username", value);
        }
        wrapper.orderByAsc("id").last("LIMIT 20");

        return userMapper.selectList(wrapper).stream()
                .map(user -> toSearchVO(userId, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendRequest(Long userId, SendFriendRequestDTO dto) {
        if (dto.getToUserId().equals(userId)) {
            throw new BusinessException(400, "不能添加自己为好友");
        }
        User target = userMapper.selectById(dto.getToUserId());
        if (target == null || Boolean.TRUE.equals(target.getDeleted())) {
            throw new NotFoundException(404, "用户不存在");
        }
        if (isFriend(userId, target.getId())) {
            throw new BusinessException(400, "你们已经是好友了");
        }
        if (requestMapper.selectCount(new QueryWrapper<ImFriendRequest>()
                .eq("from_user_id", target.getId())
                .eq("to_user_id", userId)
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)) > 0) {
            throw new BusinessException(400, "对方已向你发送好友申请，请先在好友申请中处理");
        }

        String applyMessage = dto.getApplyMessage() == null ? null : dto.getApplyMessage().trim();
        ImFriendRequest existing = requestMapper.selectOne(new QueryWrapper<ImFriendRequest>()
                .eq("from_user_id", userId)
                .eq("to_user_id", target.getId())
                .eq("deleted", false));
        if (existing != null) {
            if (STATUS_PENDING.equals(existing.getStatus())) {
                throw new BusinessException(400, "已发送好友申请，请等待对方同意");
            }
            existing.setStatus(STATUS_PENDING);
            existing.setApplyMessage(applyMessage);
            existing.setUpdatedAt(LocalDateTime.now());
            requestMapper.updateById(existing);
        } else {
            ImFriendRequest request = ImFriendRequest.builder()
                    .fromUserId(userId)
                    .toUserId(target.getId())
                    .applyMessage(applyMessage)
                    .status(STATUS_PENDING)
                    .build();
            request.setCreatedAt(LocalDateTime.now());
            requestMapper.insert(request);
        }
        log.info("发送好友申请 - 申请: {}, 接收: {}", userId, target.getId());
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests(Long userId) {
        List<ImFriendRequest> requests = requestMapper.selectList(new QueryWrapper<ImFriendRequest>()
                .eq("to_user_id", userId)
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)
                .orderByDesc("id"));
        return toRequestVOList(requests);
    }

    @Override
    public List<FriendRequestVO> listSentRequests(Long userId) {
        List<ImFriendRequest> requests = requestMapper.selectList(new QueryWrapper<ImFriendRequest>()
                .eq("from_user_id", userId)
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)
                .orderByDesc("id"));
        return toRequestVOList(requests);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRequest(Long userId, Long requestId, boolean approve) {
        ImFriendRequest request = requestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getToUserId(), userId)
                || Boolean.TRUE.equals(request.getDeleted())) {
            throw new NotFoundException(404, "好友申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已处理");
        }

        User applicant = userMapper.selectById(request.getFromUserId());
        if (applicant == null || Boolean.TRUE.equals(applicant.getDeleted())) {
            throw new NotFoundException(404, "申请用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (approve) {
            saveFriend(userId, request.getFromUserId(), now);
            saveFriend(request.getFromUserId(), userId, now);
            // 对方之前也向我发过申请时，一并置为已通过
            requestMapper.update(null, new UpdateWrapper<ImFriendRequest>()
                    .eq("from_user_id", userId)
                    .eq("to_user_id", request.getFromUserId())
                    .eq("status", STATUS_PENDING)
                    .eq("deleted", false)
                    .set("status", STATUS_APPROVED)
                    .set("updated_at", now));
            request.setStatus(STATUS_APPROVED);
        } else {
            request.setStatus(STATUS_REJECTED);
        }
        request.setUpdatedAt(now);
        requestMapper.updateById(request);
        log.info("审核好友申请 - 申请ID: {}, 结果: {}", requestId, request.getStatus());
    }

    @Override
    public List<FriendVO> listFriends(Long userId) {
        List<ImFriend> friends = friendMapper.selectList(new QueryWrapper<ImFriend>()
                .eq("user_id", userId)
                .eq("deleted", false)
                .orderByDesc("created_at"));
        if (friends.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(
                        friends.stream().map(ImFriend::getFriendId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, ImChatState> stateMap = chatStateMapper.selectList(new QueryWrapper<ImChatState>()
                        .eq("user_id", userId)
                        .eq("conversation_type", "PRIVATE")
                        .eq("deleted", false))
                .stream()
                .collect(Collectors.toMap(ImChatState::getTargetId, state -> state));

        return friends.stream()
                .map(friend -> {
                    User user = userMap.get(friend.getFriendId());
                    if (user == null) {
                        return null;
                    }
                    // 查询与该好友的最后一条消息，用于会话列表展示
                    ImPrivateMessage lastMessage = messageMapper.selectOne(new QueryWrapper<ImPrivateMessage>()
                            .eq("deleted", false)
                            .and(w -> w
                                    .and(x -> x.eq("from_user_id", userId).eq("to_user_id", friend.getFriendId()))
                                    .or(x -> x.eq("from_user_id", friend.getFriendId()).eq("to_user_id", userId)))
                            .orderByDesc("id")
                            .last("LIMIT 1"));
                    ImChatState state = stateMap.get(friend.getFriendId());
                    long lastRead = state == null || state.getLastReadMessageId() == null
                            ? 0L : state.getLastReadMessageId();
                    int unread = messageMapper.selectCount(new QueryWrapper<ImPrivateMessage>()
                            .eq("to_user_id", userId)
                            .eq("from_user_id", friend.getFriendId())
                            .eq("deleted", false)
                            .gt("id", lastRead)).intValue();
                    return FriendVO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .avatar(user.getAvatar())
                            .mobile(maskMobile(user.getMobile()))
                            .remark(friend.getRemark())
                            .lastMessage(lastMessage == null ? null : lastMessage.getContent())
                            .lastMessageTime(lastMessage == null ? null : lastMessage.getCreatedAt())
                            .unreadCount(unread)
                            .pinned(state != null && Boolean.TRUE.equals(state.getPinned()))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void updateRemark(Long userId, Long friendId, String remark) {
        ImFriend friend = friendMapper.selectOne(new QueryWrapper<ImFriend>()
                .eq("user_id", userId)
                .eq("friend_id", friendId)
                .eq("deleted", false));
        if (friend == null) {
            throw new NotFoundException(404, "好友不存在");
        }

        String value = remark == null ? null : remark.trim();
        friend.setRemark(value == null || value.isEmpty() ? null : value);
        friend.setUpdatedAt(LocalDateTime.now());
        friendMapper.updateById(friend);
    }

    @Override
    public List<PrivateMessageVO> listMessages(Long userId, Long friendId, Long beforeId, Integer limit) {
        requireFriend(userId, friendId);

        int size = limit == null ? 50 : Math.min(Math.max(limit, 1), 100);
        QueryWrapper<ImPrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        wrapper.and(w -> w
                .and(x -> x.eq("from_user_id", userId).eq("to_user_id", friendId))
                .or(x -> x.eq("from_user_id", friendId).eq("to_user_id", userId)));
        if (beforeId != null) {
            wrapper.lt("id", beforeId);
        }
        wrapper.orderByDesc("id").last("LIMIT " + size);

        List<PrivateMessageVO> list = messageMapper.selectList(wrapper).stream()
                .map(this::toMessageVO)
                .collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageVO sendMessage(Long userId, Long friendId, SendPrivateMessageDTO dto) {
        requireFriend(userId, friendId);

        String content = dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }

        User sender = userMapper.selectById(userId);
        ImPrivateMessage message = ImPrivateMessage.builder()
                .fromUserId(userId)
                .toUserId(friendId)
                .messageType(TYPE_TEXT)
                .content(content)
                .build();
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        PrivateMessageVO vo = toMessageVO(message);
        MessageSendDto ws = new MessageSendDto();
        ws.setMessageType(WsMessageType.PRIVATE_MESSAGE.name());
        ws.setTargetScope(MessageSend2TypeEnum.PRIVATE.getCode());
        ws.setRecevedId(friendId);
        ws.setSenderId(userId);
        ws.setSenderName(sender != null ? sender.getUsername() : null);
        ws.setContent(JsonUtils.toJson(vo));
        ws.setSentAt(vo.getCreatedAt());
        channelContextUtils.sendMessage(ws);
        return vo;
    }

    private FriendSearchVO toSearchVO(Long userId, User user) {
        String relation = RELATION_NONE;
        if (isFriend(userId, user.getId())) {
            relation = RELATION_FRIEND;
        } else if (requestMapper.selectCount(new QueryWrapper<ImFriendRequest>()
                .eq("from_user_id", userId)
                .eq("to_user_id", user.getId())
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)) > 0) {
            relation = RELATION_REQUEST_SENT;
        } else if (requestMapper.selectCount(new QueryWrapper<ImFriendRequest>()
                .eq("from_user_id", user.getId())
                .eq("to_user_id", userId)
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)) > 0) {
            relation = RELATION_REQUEST_RECEIVED;
        }

        return FriendSearchVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .mobile(maskMobile(user.getMobile()))
                .relation(relation)
                .build();
    }

    private List<FriendRequestVO> toRequestVOList(List<ImFriendRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = requests.stream()
                .flatMap(request -> java.util.stream.Stream.of(request.getFromUserId(), request.getToUserId()))
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return requests.stream()
                .map(request -> {
                    User applicant = userMap.get(request.getFromUserId());
                    User receiver = userMap.get(request.getToUserId());
                    return FriendRequestVO.builder()
                            .id(request.getId())
                            .fromUserId(request.getFromUserId())
                            .username(applicant != null ? applicant.getUsername() : "未知用户")
                            .toUserId(request.getToUserId())
                            .toUsername(receiver != null ? receiver.getUsername() : "未知用户")
                            .avatar(applicant != null ? applicant.getAvatar() : null)
                            .applyMessage(request.getApplyMessage())
                            .status(request.getStatus())
                            .createdAt(request.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private PrivateMessageVO toMessageVO(ImPrivateMessage message) {
        return PrivateMessageVO.builder()
                .id(message.getId())
                .fromUserId(message.getFromUserId())
                .toUserId(message.getToUserId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void saveFriend(Long userId, Long friendId, LocalDateTime now) {
        ImFriend existing = friendMapper.selectOne(new QueryWrapper<ImFriend>()
                .eq("user_id", userId)
                .eq("friend_id", friendId));
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                existing.setDeleted(false);
                existing.setUpdatedAt(now);
                friendMapper.updateById(existing);
            }
            return;
        }
        ImFriend friend = ImFriend.builder()
                .userId(userId)
                .friendId(friendId)
                .build();
        friend.setCreatedAt(now);
        friendMapper.insert(friend);
    }

    private boolean isFriend(Long userId, Long friendId) {
        return friendMapper.selectCount(new QueryWrapper<ImFriend>()
                .eq("user_id", userId)
                .eq("friend_id", friendId)
                .eq("deleted", false)) > 0;
    }

    private void requireFriend(Long userId, Long friendId) {
        if (friendId == null) {
            throw new BusinessException(400, "好友ID不能为空");
        }
        if (userMapper.selectById(friendId) == null) {
            throw new NotFoundException(404, "用户不存在");
        }
        if (!isFriend(userId, friendId)) {
            throw new BusinessException(400, "对方还不是你的好友");
        }
    }

    /**
     * 手机号归一化为 +86 前缀格式
     */
    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String value = mobile.trim().replace(" ", "");
        return value.startsWith("+86") ? value : "+86" + value;
    }

    /**
     * 手机号脱敏展示
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return null;
        }
        String digits = mobile.startsWith("+86") ? mobile.substring(3) : mobile;
        if (digits.length() < 7) {
            return mobile;
        }
        return "+86" + digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }
}
