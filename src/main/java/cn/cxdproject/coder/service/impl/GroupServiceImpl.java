package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.ImChatStateMapper;
import cn.cxdproject.coder.mapper.ImFriendMapper;
import cn.cxdproject.coder.mapper.ImFriendRequestMapper;
import cn.cxdproject.coder.mapper.ImGroupJoinRequestMapper;
import cn.cxdproject.coder.mapper.ImGroupMapper;
import cn.cxdproject.coder.mapper.ImGroupMemberMapper;
import cn.cxdproject.coder.mapper.ImGroupMessageMapper;
import cn.cxdproject.coder.mapper.ImGroupAppealMapper;
import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.AddGroupMembersDTO;
import cn.cxdproject.coder.model.dto.ApplyJoinGroupDTO;
import cn.cxdproject.coder.model.dto.CreateGroupDTO;
import cn.cxdproject.coder.model.dto.GroupAppealDTO;
import cn.cxdproject.coder.model.dto.InviteGroupMembersDTO;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.dto.SendGroupMessageDTO;
import cn.cxdproject.coder.model.entity.ImChatState;
import cn.cxdproject.coder.model.entity.ImFriend;
import cn.cxdproject.coder.model.entity.ImFriendRequest;
import cn.cxdproject.coder.model.entity.ImGroup;
import cn.cxdproject.coder.model.entity.ImGroupAppeal;
import cn.cxdproject.coder.model.entity.ImGroupJoinRequest;
import cn.cxdproject.coder.model.entity.ImGroupMember;
import cn.cxdproject.coder.model.entity.ImGroupMessage;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.GroupJoinRequestVO;
import cn.cxdproject.coder.model.vo.GroupMemberVO;
import cn.cxdproject.coder.model.vo.GroupMessageVO;
import cn.cxdproject.coder.model.vo.GroupSearchVO;
import cn.cxdproject.coder.model.vo.GroupVO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.vo.FileVO;
import cn.cxdproject.coder.service.GroupService;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import cn.cxdproject.coder.common.storage.FileStorageAdapter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 群聊服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_MEMBER = "member";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_FILE = "FILE";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String GROUP_STATUS_BANNED = "banned";

    private static final String RELATION_NONE = "NONE";
    private static final String RELATION_FRIEND = "FRIEND";
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";

    private final ImGroupMapper groupMapper;
    private final ImGroupMemberMapper memberMapper;
    private final ImGroupMessageMapper messageMapper;
    private final ImGroupJoinRequestMapper joinRequestMapper;
    private final UserMapper userMapper;
    private final ChannelContextUtils channelContextUtils;
    private final FileStorageAdapter fileStorageAdapter;
    private final ImChatStateMapper chatStateMapper;
    private final ImFriendMapper friendMapper;
    private final ImFriendRequestMapper friendRequestMapper;
    private final ImGroupAppealMapper appealMapper;

    public GroupServiceImpl(ImGroupMapper groupMapper,
                            ImGroupMemberMapper memberMapper,
                            ImGroupMessageMapper messageMapper,
                            ImGroupJoinRequestMapper joinRequestMapper,
                            UserMapper userMapper,
                            ChannelContextUtils channelContextUtils,
                            FileStorageAdapter fileStorageAdapter,
                            ImChatStateMapper chatStateMapper,
                            ImFriendMapper friendMapper,
                            ImFriendRequestMapper friendRequestMapper,
                            ImGroupAppealMapper appealMapper) {
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.joinRequestMapper = joinRequestMapper;
        this.userMapper = userMapper;
        this.channelContextUtils = channelContextUtils;
        this.fileStorageAdapter = fileStorageAdapter;
        this.chatStateMapper = chatStateMapper;
        this.friendMapper = friendMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.appealMapper = appealMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(Long userId, CreateGroupDTO dto) {
        ImGroup group = ImGroup.builder()
                .groupName(dto.getGroupName().trim())
                .groupNo(generateGroupNo())
                .ownerId(userId)
                .status("normal")
                .build();
        group.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(group);

        // 群主默认加入
        addMember(group.getId(), userId, ROLE_OWNER);

        // 添加初始成员
        List<String> usernames = normalizeUsernames(dto.getMemberUsernames());
        if (!usernames.isEmpty()) {
            User owner = userMapper.selectById(userId);
            if (owner != null) {
                usernames.remove(owner.getUsername());
            }
            List<User> users = findUsersByUsernames(usernames);
            for (User user : users) {
                addMember(group.getId(), user.getId(), ROLE_MEMBER);
            }
        }

        saveSystemMessage(group.getId(), "群聊创建成功");
        log.info("创建群聊成功 - 群ID: {}, 群主: {}", group.getId(), userId);

        return toGroupVO(group, userId);
    }

    @Override
    public List<GroupVO> listMyGroups(Long userId) {
        List<ImGroupMember> memberships = memberMapper.selectList(
                new QueryWrapper<ImGroupMember>()
                        .eq("user_id", userId)
                        .eq("deleted", false)
                        .orderByDesc("id"));
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = memberships.stream()
                .map(ImGroupMember::getGroupId)
                .collect(Collectors.toList());
        List<ImGroup> groups = groupMapper.selectList(
                new QueryWrapper<ImGroup>()
                        .in("id", groupIds)
                        .eq("deleted", false)
                        .orderByDesc("id"));
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        // 群主名称批量查询，避免逐个查库
        List<Long> ownerIds = groups.stream()
                .map(ImGroup::getOwnerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> ownerMap = userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, ImChatState> stateMap = chatStateMapper.selectList(new QueryWrapper<ImChatState>()
                        .eq("user_id", userId)
                        .eq("conversation_type", "GROUP")
                        .eq("deleted", false))
                .stream()
                .collect(Collectors.toMap(ImChatState::getTargetId, state -> state));

        List<GroupVO> result = new ArrayList<>();
        for (ImGroup group : groups) {
            GroupVO vo = toGroupVO(group, userId);
            User owner = ownerMap.get(group.getOwnerId());
            if (owner != null) {
                vo.setOwnerName(owner.getUsername());
            }
            ImChatState state = stateMap.get(group.getId());
            long lastRead = state == null || state.getLastReadMessageId() == null
                    ? 0L : state.getLastReadMessageId();
            int unread = messageMapper.selectCount(new QueryWrapper<ImGroupMessage>()
                    .eq("group_id", group.getId())
                    .eq("deleted", false)
                    .ne("sender_id", userId)
                    .gt("id", lastRead)).intValue();
            vo.setUnreadCount(unread);
            vo.setPinned(state != null && Boolean.TRUE.equals(state.getPinned()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public GroupVO getGroupDetail(Long userId, Long groupId) {
        ImGroup group = requireGroup(groupId);
        requireMember(groupId, userId);
        return toGroupVO(group, userId);
    }

    @Override
    public GroupSearchVO searchByGroupNo(Long userId, String groupNo) {
        ImGroup group = groupMapper.selectOne(new QueryWrapper<ImGroup>()
                .eq("group_no", groupNo)
                .eq("deleted", false));
        if (group == null) {
            throw new NotFoundException(404, "未找到该群聊，请检查群号");
        }

        User owner = userMapper.selectById(group.getOwnerId());
        int memberCount = memberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                .eq("group_id", group.getId())
                .eq("deleted", false)).intValue();

        boolean joined = isMember(group.getId(), userId);
        boolean pending = !joined && joinRequestMapper.selectCount(new QueryWrapper<ImGroupJoinRequest>()
                .eq("group_id", group.getId())
                .eq("user_id", userId)
                .eq("status", STATUS_PENDING)
                .eq("deleted", false)) > 0;

        return GroupSearchVO.builder()
                .id(group.getId())
                .groupNo(group.getGroupNo())
                .groupName(group.getGroupName())
                .ownerName(owner != null ? owner.getUsername() : "未知用户")
                .memberCount(memberCount)
                .announcement(group.getAnnouncement())
                .joined(joined)
                .pending(pending)
                .status(group.getStatus())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoin(Long userId, ApplyJoinGroupDTO dto) {
        ImGroup group = groupMapper.selectOne(new QueryWrapper<ImGroup>()
                .eq("group_no", dto.getGroupNo())
                .eq("deleted", false));
        if (group == null) {
            throw new NotFoundException(404, "未找到该群聊，请检查群号");
        }
        // 管理员可直接入群，无需群主审核
        if (isAdmin(userMapper.selectById(userId))) {
            if (!isMember(group.getId(), userId)) {
                addMember(group.getId(), userId, ROLE_MEMBER);
            }
            log.info("管理员直接加入群聊 - 群ID: {}, 管理员: {}", group.getId(), userId);
            return;
        }
        ensureGroupActive(group);
        if (isMember(group.getId(), userId)) {
            throw new BusinessException(400, "你已在该群聊中");
        }

        ImGroupJoinRequest existing = joinRequestMapper.selectOne(new QueryWrapper<ImGroupJoinRequest>()
                .eq("group_id", group.getId())
                .eq("user_id", userId)
                .eq("deleted", false));
        if (existing != null && STATUS_PENDING.equals(existing.getStatus())) {
            throw new BusinessException(400, "已提交申请，请等待群主审核");
        }

        if (existing != null) {
            // 之前被拒绝或已退出群聊，重新提交主动申请（清空邀请人）
            joinRequestMapper.update(null, new UpdateWrapper<ImGroupJoinRequest>()
                    .eq("id", existing.getId())
                    .set("status", STATUS_PENDING)
                    .set("inviter_id", null)
                    .set("updated_at", LocalDateTime.now()));
        } else {
            ImGroupJoinRequest request = ImGroupJoinRequest.builder()
                    .groupId(group.getId())
                    .userId(userId)
                    .status(STATUS_PENDING)
                    .build();
            request.setCreatedAt(LocalDateTime.now());
            joinRequestMapper.insert(request);
        }
        log.info("提交入群申请 - 群ID: {}, 用户: {}", group.getId(), userId);
    }

    @Override
    public List<GroupJoinRequestVO> listJoinRequests(Long userId, Long groupId) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);

        List<ImGroupJoinRequest> requests = joinRequestMapper.selectList(
                new QueryWrapper<ImGroupJoinRequest>()
                        .eq("group_id", groupId)
                        .eq("status", STATUS_PENDING)
                        .eq("deleted", false)
                        .orderByAsc("id"));
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(
                        requests.stream().map(ImGroupJoinRequest::getUserId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Long> inviterIds = requests.stream()
                .map(ImGroupJoinRequest::getInviterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> inviterMap = inviterIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(inviterIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));

        return requests.stream()
                .map(request -> {
                    User applicant = userMap.get(request.getUserId());
                    User inviter = request.getInviterId() == null ? null : inviterMap.get(request.getInviterId());
                    return GroupJoinRequestVO.builder()
                            .id(request.getId())
                            .groupId(request.getGroupId())
                            .userId(request.getUserId())
                            .username(applicant != null ? applicant.getUsername() : "未知用户")
                            .avatar(applicant != null ? applicant.getAvatar() : null)
                            .inviterId(request.getInviterId())
                            .inviterName(inviter != null ? inviter.getUsername() : null)
                            .status(request.getStatus())
                            .createdAt(request.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewJoinRequest(Long userId, Long groupId, Long requestId, boolean approve) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);

        ImGroupJoinRequest request = joinRequestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getGroupId(), groupId)
                || Boolean.TRUE.equals(request.getDeleted())) {
            throw new NotFoundException(404, "入群申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已处理");
        }

        User applicant = userMapper.selectById(request.getUserId());
        if (applicant == null || Boolean.TRUE.equals(applicant.getDeleted())) {
            throw new NotFoundException(404, "申请用户不存在");
        }

        if (approve) {
            ensureGroupActive(group);
            if (!isMember(groupId, request.getUserId())) {
                addMember(groupId, request.getUserId(), ROLE_MEMBER);
                saveSystemMessage(groupId, applicant.getUsername() + " 加入了群聊");
            }
            request.setStatus(STATUS_APPROVED);
        } else {
            request.setStatus(STATUS_REJECTED);
        }
        request.setUpdatedAt(LocalDateTime.now());
        joinRequestMapper.updateById(request);
        log.info("审核入群申请 - 群ID: {}, 申请人: {}, 结果: {}", groupId, request.getUserId(),
                approve ? STATUS_APPROVED : STATUS_REJECTED);
    }

    @Override
    public List<GroupMemberVO> listMembers(Long userId, Long groupId) {
        requireGroup(groupId);
        requireMember(groupId, userId);

        List<ImGroupMember> members = memberMapper.selectList(
                new QueryWrapper<ImGroupMember>()
                        .eq("group_id", groupId)
                        .eq("deleted", false)
                        .orderByAsc("join_time"));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = members.stream()
                .map(ImGroupMember::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 当前用户与群成员的好友关系，用于在群成员列表中发起加好友
        Set<Long> friendIds = friendMapper.selectList(new QueryWrapper<ImFriend>()
                        .eq("user_id", userId)
                        .eq("deleted", false))
                .stream()
                .map(ImFriend::getFriendId)
                .collect(Collectors.toSet());

        Set<Long> sentTo = new HashSet<>();
        Set<Long> receivedFrom = new HashSet<>();
        List<ImFriendRequest> pendingRequests = friendRequestMapper.selectList(
                new QueryWrapper<ImFriendRequest>()
                        .eq("status", STATUS_PENDING)
                        .eq("deleted", false)
                        .and(w -> w.eq("from_user_id", userId).or().eq("to_user_id", userId)));
        for (ImFriendRequest request : pendingRequests) {
            if (Objects.equals(request.getFromUserId(), userId)) {
                sentTo.add(request.getToUserId());
            } else if (Objects.equals(request.getToUserId(), userId)) {
                receivedFrom.add(request.getFromUserId());
            }
        }

        return members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    String relation = RELATION_NONE;
                    if (friendIds.contains(member.getUserId())) {
                        relation = RELATION_FRIEND;
                    } else if (sentTo.contains(member.getUserId())) {
                        relation = RELATION_REQUEST_SENT;
                    } else if (receivedFrom.contains(member.getUserId())) {
                        relation = RELATION_REQUEST_RECEIVED;
                    }
                    return GroupMemberVO.builder()
                            .userId(member.getUserId())
                            .username(user != null ? user.getUsername() : "未知用户")
                            .avatar(user != null ? user.getAvatar() : null)
                            .roleType(member.getRoleType())
                            .joinTime(member.getJoinTime())
                            .relation(relation)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long userId, Long groupId, AddGroupMembersDTO dto) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);
        ensureGroupActive(group);

        List<String> usernames = normalizeUsernames(dto.getMemberUsernames());
        if (usernames.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的成员");
        }

        List<User> users = findUsersByUsernames(usernames);
        List<String> addedNames = new ArrayList<>();
        for (User user : users) {
            if (isMember(groupId, user.getId())) {
                continue;
            }
            addMember(groupId, user.getId(), ROLE_MEMBER);
            addedNames.add(user.getUsername());
        }

        if (addedNames.isEmpty()) {
            throw new BusinessException(400, "所选用户都已在群聊中");
        }

        saveSystemMessage(groupId, String.join("、", addedNames) + " 加入了群聊");
        log.info("添加群成员 - 群ID: {}, 操作人: {}, 新增: {}", groupId, userId, addedNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMembers(Long userId, Long groupId, InviteGroupMembersDTO dto) {
        ImGroup group = requireGroup(groupId);
        requireMember(groupId, userId);
        ensureGroupActive(group);

        boolean inviterIsOwner = Objects.equals(group.getOwnerId(), userId);
        List<String> addedNames = new ArrayList<>();
        List<String> pendingNames = new ArrayList<>();

        for (Long targetUserId : dto.getUserIds()) {
            if (targetUserId == null || Objects.equals(targetUserId, userId)
                    || isMember(groupId, targetUserId)) {
                continue;
            }
            User target = userMapper.selectById(targetUserId);
            if (target == null || Boolean.TRUE.equals(target.getDeleted())) {
                continue;
            }

            if (inviterIsOwner) {
                addMember(groupId, targetUserId, ROLE_MEMBER);
                addedNames.add(target.getUsername());
                continue;
            }

            // 普通成员邀请：生成待审核入群申请，记录邀请人
            ImGroupJoinRequest existing = joinRequestMapper.selectOne(new QueryWrapper<ImGroupJoinRequest>()
                    .eq("group_id", groupId)
                    .eq("user_id", targetUserId)
                    .eq("deleted", false));
            if (existing != null && STATUS_PENDING.equals(existing.getStatus())) {
                continue;
            }
            if (existing != null) {
                existing.setStatus(STATUS_PENDING);
                existing.setInviterId(userId);
                existing.setUpdatedAt(LocalDateTime.now());
                joinRequestMapper.updateById(existing);
            } else {
                ImGroupJoinRequest request = ImGroupJoinRequest.builder()
                        .groupId(groupId)
                        .userId(targetUserId)
                        .inviterId(userId)
                        .status(STATUS_PENDING)
                        .build();
                request.setCreatedAt(LocalDateTime.now());
                joinRequestMapper.insert(request);
            }
            pendingNames.add(target.getUsername());
        }

        if (addedNames.isEmpty() && pendingNames.isEmpty()) {
            throw new BusinessException(400, "所选好友都已在群聊中或已提交申请");
        }
        if (!addedNames.isEmpty()) {
            saveSystemMessage(groupId, String.join("、", addedNames) + " 加入了群聊");
        }
        log.info("邀请好友入群 - 群ID: {}, 邀请人: {}, 直接加入: {}, 待审核: {}",
                groupId, userId, addedNames, pendingNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);

        if (userId.equals(targetUserId)) {
            throw new BusinessException(400, "群主不能移除自己，请使用解散群聊");
        }
        if (!isMember(groupId, targetUserId)) {
            throw new NotFoundException(404, "该用户不在群聊中");
        }

        memberMapper.delete(new QueryWrapper<ImGroupMember>()
                .eq("group_id", groupId)
                .eq("user_id", targetUserId));

        User target = userMapper.selectById(targetUserId);
        saveSystemMessage(groupId, (target != null ? target.getUsername() : "成员") + " 被移出群聊");
        log.info("移除群成员 - 群ID: {}, 操作人: {}, 目标: {}", groupId, userId, targetUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long userId, Long groupId) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);

        group.setDeleted(true);
        groupMapper.updateById(group);
        memberMapper.delete(new QueryWrapper<ImGroupMember>().eq("group_id", groupId));

        log.info("解散群聊 - 群ID: {}, 群主: {}", groupId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long userId, Long groupId) {
        ImGroup group = requireGroup(groupId);
        requireMember(groupId, userId);

        if (Objects.equals(group.getOwnerId(), userId)) {
            throw new BusinessException(400, "群主不能退出群聊，请先解散群聊");
        }

        memberMapper.delete(new QueryWrapper<ImGroupMember>()
                .eq("group_id", groupId)
                .eq("user_id", userId));

        User user = userMapper.selectById(userId);
        saveSystemMessage(groupId, (user != null ? user.getUsername() : "成员") + " 退出了群聊");
        log.info("退出群聊 - 群ID: {}, 用户: {}", groupId, userId);
    }

    @Override
    public List<GroupMessageVO> listMessages(Long userId, Long groupId, Long beforeId, Integer limit) {
        requireGroup(groupId);
        requireMember(groupId, userId);

        int size = limit == null ? 20 : Math.min(Math.max(limit, 1), 50);
        QueryWrapper<ImGroupMessage> wrapper = new QueryWrapper<ImGroupMessage>()
                .eq("group_id", groupId)
                .eq("deleted", false);
        if (beforeId != null) {
            wrapper.lt("id", beforeId);
        }
        wrapper.orderByDesc("id").last("LIMIT " + size);

        List<ImGroupMessage> messages = messageMapper.selectList(wrapper);
        Collections.reverse(messages);

        // 批量加载发送者信息，补充头像
        List<Long> senderIds = messages.stream()
                .map(ImGroupMessage::getSenderId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));

        return messages.stream()
                .map(message -> toMessageVO(message, userMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupMessageVO sendMessage(Long userId, Long groupId, SendGroupMessageDTO dto) {
        ImGroup group = requireGroup(groupId);
        requireMember(groupId, userId);
        ensureGroupActive(group);

        User user = userMapper.selectById(userId);
        String messageType = resolveMessageType(dto);

        String content;
        if (TYPE_IMAGE.equals(messageType)) {
            content = "[图片]";
        } else if (TYPE_FILE.equals(messageType)) {
            content = "[文件] " + (dto.getFileName() != null ? dto.getFileName() : "");
        } else {
            content = dto.getContent().trim();
        }

        String fileUrl = TYPE_TEXT.equals(messageType) ? null : dto.getFileUrl().trim();

        ImGroupMessage message = ImGroupMessage.builder()
                .groupId(groupId)
                .senderId(userId)
                .senderName(user != null ? user.getUsername() : "用户" + userId)
                .messageType(messageType)
                .content(content)
                .fileUrl(fileUrl)
                .fileName(TYPE_TEXT.equals(messageType) ? null : dto.getFileName())
                .fileSize(TYPE_TEXT.equals(messageType) ? null : dto.getFileSize())
                .build();
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        GroupMessageVO vo = toMessageVO(message, null);
        if (user != null) {
            vo.setSenderAvatar(user.getAvatar());
        }
        broadcast(group.getId(), vo, userId);
        return vo;
    }

    @Override
    public FileVO uploadAttachment(Long userId, Long groupId, MultipartFile file) {
        ImGroup group = requireGroup(groupId);
        requireMember(groupId, userId);
        ensureGroupActive(group);

        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }

        FileVO fileVO = fileStorageAdapter.upload("group", file);
        fileVO.setOriginUrl(toAccessUrl(fileVO.getOriginUrl()));
        if (fileVO.getThumbUrl() != null) {
            fileVO.setThumbUrl(toAccessUrl(fileVO.getThumbUrl()));
        }
        log.info("群聊附件上传成功 - 群ID: {}, 用户: {}, 文件: {}", groupId, userId, file.getOriginalFilename());
        return fileVO;
    }

    /**
     * 校验并归一化消息类型
     */
    private String resolveMessageType(SendGroupMessageDTO dto) {
        String type = dto.getMessageType() == null ? TYPE_TEXT : dto.getMessageType().trim().toUpperCase();
        if (TYPE_TEXT.equals(type)) {
            if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                throw new BusinessException(400, "消息内容不能为空");
            }
            return TYPE_TEXT;
        }
        if (!TYPE_IMAGE.equals(type) && !TYPE_FILE.equals(type)) {
            throw new BusinessException(400, "不支持的消息类型");
        }
        if (dto.getFileUrl() == null || dto.getFileUrl().trim().isEmpty()) {
            throw new BusinessException(400, "附件地址不能为空");
        }
        // 只允许引用本地上传的文件，避免把任意外链当作附件
        String url = dto.getFileUrl().trim();
        if (!url.startsWith("/api/files/") && !url.startsWith("/files/")) {
            throw new BusinessException(400, "附件地址不合法");
        }
        return type;
    }

    /**
     * 存储层返回 /files/...，实际访问路径挂在 /api/files/... 下
     */
    private String toAccessUrl(String url) {
        if (url == null || url.isEmpty() || url.startsWith("/api/")) {
            return url;
        }
        return url.startsWith("/files/") ? "/api" + url : url;
    }

    /**
     * 向群内所有在线成员推送消息，排除指定用户
     */
    private void broadcast(Long groupId, GroupMessageVO vo, Long excludeUserId) {
        List<ImGroupMember> members = memberMapper.selectList(
                new QueryWrapper<ImGroupMember>()
                        .eq("group_id", groupId)
                        .eq("deleted", false));
        String content = JsonUtils.toJson(vo);

        for (ImGroupMember member : members) {
            if (excludeUserId != null && excludeUserId.equals(member.getUserId())) {
                continue;
            }
            MessageSendDto dto = new MessageSendDto();
            dto.setMessageType(WsMessageType.GROUP_MESSAGE.name());
            dto.setTargetScope(MessageSend2TypeEnum.PRIVATE.getCode());
            dto.setRecevedId(member.getUserId());
            dto.setSenderId(vo.getSenderId());
            dto.setSenderName(vo.getSenderName());
            dto.setContent(content);
            dto.setSentAt(vo.getCreatedAt());
            channelContextUtils.sendMessage(dto);
        }
    }

    /**
     * 保存并广播系统消息
     */
    private void saveSystemMessage(Long groupId, String content) {
        ImGroupMessage message = ImGroupMessage.builder()
                .groupId(groupId)
                .senderId(0L)
                .senderName("系统")
                .messageType(WsMessageType.SYSTEM.name())
                .content(content)
                .build();
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        broadcast(groupId, toMessageVO(message, null), null);
    }

    private void addMember(Long groupId, Long userId, String roleType) {
        ImGroupMember member = ImGroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .roleType(roleType)
                .joinTime(LocalDateTime.now())
                .build();
        memberMapper.insert(member);
    }

    private boolean isMember(Long groupId, Long userId) {
        return memberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                .eq("group_id", groupId)
                .eq("user_id", userId)
                .eq("deleted", false)) > 0;
    }

    private void requireMember(Long groupId, Long userId) {
        if (!isMember(groupId, userId)) {
            throw new BusinessException(400, "你不在该群聊中");
        }
    }

    private void requireOwner(ImGroup group, Long userId) {
        if (!Objects.equals(group.getOwnerId(), userId)) {
            throw new BusinessException(400, "只有群主可以执行该操作");
        }
    }

    /**
     * 生成唯一的 10 位数字群号
     */
    private String generateGroupNo() {
        for (int i = 0; i < 20; i++) {
            long value = 1_000_000_000L + ThreadLocalRandom.current().nextLong(9_000_000_000L);
            String groupNo = String.valueOf(value);
            if (groupMapper.selectCount(new QueryWrapper<ImGroup>().eq("group_no", groupNo)) == 0) {
                return groupNo;
            }
        }
        throw new BusinessException(500, "群号生成失败，请稍后重试");
    }

    private ImGroup requireGroup(Long groupId) {
        ImGroup group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getDeleted())) {
            throw new NotFoundException(404, "群聊不存在");
        }
        return group;
    }

    /**
     * 已封禁的群聊禁止加入和发送消息
     */
    private void ensureGroupActive(ImGroup group) {
        if (GROUP_STATUS_BANNED.equals(group.getStatus())) {
            throw new BusinessException(400, "该群聊已被封禁");
        }
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole().trim();
        return "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    private List<String> normalizeUsernames(List<String> usernames) {
        if (usernames == null) {
            return new ArrayList<>();
        }
        return usernames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按用户名批量查询用户，存在不存在的用户名时直接报错
     */
    private List<User> findUsersByUsernames(List<String> usernames) {
        if (usernames.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(new QueryWrapper<User>().in("username", usernames));
        if (users.size() < usernames.size()) {
            List<String> missing = usernames.stream()
                    .filter(name -> users.stream().noneMatch(user -> name.equals(user.getUsername())))
                    .collect(Collectors.toList());
            throw new BusinessException(400, "用户不存在: " + String.join("、", missing));
        }
        return users;
    }

    private GroupVO toGroupVO(ImGroup group, Long currentUserId) {
        User owner = userMapper.selectById(group.getOwnerId());
        int memberCount = memberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                .eq("group_id", group.getId())
                .eq("deleted", false)).intValue();

        ImGroupMember membership = memberMapper.selectOne(new QueryWrapper<ImGroupMember>()
                .eq("group_id", group.getId())
                .eq("user_id", currentUserId)
                .eq("deleted", false));

        List<ImGroupMessage> latest = messageMapper.selectList(new QueryWrapper<ImGroupMessage>()
                .eq("group_id", group.getId())
                .eq("deleted", false)
                .orderByDesc("id")
                .last("LIMIT 1"));
        ImGroupMessage lastMessage = latest.isEmpty() ? null : latest.get(0);

        return GroupVO.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupNo(group.getGroupNo())
                .ownerId(group.getOwnerId())
                .ownerName(owner != null ? owner.getUsername() : "未知用户")
                .memberCount(memberCount)
                .myRole(membership != null ? membership.getRoleType() : null)
                .announcement(group.getAnnouncement())
                .status(group.getStatus())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private GroupMessageVO toMessageVO(ImGroupMessage message, Map<Long, User> userMap) {
        User sender = userMap == null ? null : userMap.get(message.getSenderId());
        return GroupMessageVO.builder()
                .id(message.getId())
                .groupId(message.getGroupId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderAvatar(sender != null ? sender.getAvatar() : null)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAppeal(Long userId, Long groupId, GroupAppealDTO dto) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);
        if (!GROUP_STATUS_BANNED.equals(group.getStatus())) {
            throw new BusinessException(400, "该群聊未被封禁，无需申诉");
        }
        long pendingCount = appealMapper.selectCount(new QueryWrapper<ImGroupAppeal>()
                .eq("group_id", groupId)
                .eq("status", "PENDING")
                .eq("deleted", false));
        if (pendingCount > 0) {
            throw new BusinessException(400, "已有申诉正在审核中，请耐心等待");
        }

        ImGroupAppeal appeal = ImGroupAppeal.builder()
                .groupId(groupId)
                .ownerId(userId)
                .reason(dto.getReason().trim())
                .status("PENDING")
                .build();
        appealMapper.insert(appeal);
        log.info("群主提交群聊申诉 - 群ID: {}, 群主: {}", groupId, userId);
    }

    @Override
    public GroupAppealVO getLatestAppeal(Long userId, Long groupId) {
        ImGroup group = requireGroup(groupId);
        requireOwner(group, userId);

        ImGroupAppeal appeal = appealMapper.selectOne(new QueryWrapper<ImGroupAppeal>()
                .eq("group_id", groupId)
                .eq("deleted", false)
                .orderByDesc("id")
                .last("LIMIT 1"));
        if (appeal == null) {
            return null;
        }
        User owner = userMapper.selectById(appeal.getOwnerId());
        return GroupAppealVO.builder()
                .id(appeal.getId())
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .groupNo(group.getGroupNo())
                .ownerId(appeal.getOwnerId())
                .ownerName(owner != null ? owner.getUsername() : null)
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .adminRemark(appeal.getAdminRemark())
                .createdAt(appeal.getCreatedAt())
                .updatedAt(appeal.getUpdatedAt())
                .build();
    }
}
