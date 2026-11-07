package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.common.PageResponse;
import cn.cxdproject.coder.common.enums.MeetingStatus;
import cn.cxdproject.coder.common.enums.ParticipantStatus;
import cn.cxdproject.coder.config.JwtConfig;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.ImChatStateMapper;
import cn.cxdproject.coder.mapper.ImGroupAppealMapper;
import cn.cxdproject.coder.mapper.ImGroupMapper;
import cn.cxdproject.coder.mapper.ImGroupMemberMapper;
import cn.cxdproject.coder.mapper.MeetingMapper;
import cn.cxdproject.coder.mapper.MeetingParticipantMapper;
import cn.cxdproject.coder.mapper.MeetingSummaryMapper;
import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.ImChatState;
import cn.cxdproject.coder.model.entity.ImGroup;
import cn.cxdproject.coder.model.entity.ImGroupAppeal;
import cn.cxdproject.coder.model.entity.ImGroupMember;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.entity.MeetingParticipant;
import cn.cxdproject.coder.model.entity.MeetingSummary;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.AdminGroupVO;
import cn.cxdproject.coder.model.vo.AdminStatsVO;
import cn.cxdproject.coder.model.vo.AdminUserVO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.vo.MeetingSummaryVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import cn.cxdproject.coder.service.AdminService;
import cn.cxdproject.coder.service.ai.MeetingSummaryService;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.utils.RedisUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台管理服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper participantMapper;
    private final MeetingSummaryMapper summaryMapper;
    private final MeetingSummaryService summaryService;
    private final UserMapper userMapper;
    private final ImGroupMapper groupMapper;
    private final ImGroupMemberMapper groupMemberMapper;
    private final ImGroupAppealMapper groupAppealMapper;
    private final ImChatStateMapper chatStateMapper;
    private final ChannelContextUtils channelContextUtils;
    private final RedisUtils redisUtils;
    private final JwtConfig jwtConfig;

    public AdminServiceImpl(MeetingMapper meetingMapper,
                            MeetingParticipantMapper participantMapper,
                            MeetingSummaryMapper summaryMapper,
                            MeetingSummaryService summaryService,
                            UserMapper userMapper,
                            ImGroupMapper groupMapper,
                            ImGroupMemberMapper groupMemberMapper,
                            ImGroupAppealMapper groupAppealMapper,
                            ImChatStateMapper chatStateMapper,
                            ChannelContextUtils channelContextUtils,
                            RedisUtils redisUtils,
                            JwtConfig jwtConfig) {
        this.meetingMapper = meetingMapper;
        this.participantMapper = participantMapper;
        this.summaryMapper = summaryMapper;
        this.summaryService = summaryService;
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupAppealMapper = groupAppealMapper;
        this.chatStateMapper = chatStateMapper;
        this.channelContextUtils = channelContextUtils;
        this.redisUtils = redisUtils;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public AdminStatsVO getStats() {
        long totalUsers = userMapper.selectCount(new QueryWrapper<User>().eq("deleted", false));
        long activeUsers = userMapper.selectCount(new QueryWrapper<User>()
                .eq("deleted", false).eq("status", true));
        long disabledUsers = userMapper.selectCount(new QueryWrapper<User>()
                .eq("deleted", false).eq("status", false));
        long totalMeetings = meetingMapper.selectCount(new QueryWrapper<Meeting>().eq("deleted", false));
        long ongoingMeetings = meetingMapper.selectCount(new QueryWrapper<Meeting>()
                .eq("deleted", false).eq("status", MeetingStatus.ONGOING.getCode()));
        long totalGroups = groupMapper.selectCount(new QueryWrapper<ImGroup>().eq("deleted", false));
        long totalGroupMembers = groupMemberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                .eq("deleted", false));

        return AdminStatsVO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .totalMeetings(totalMeetings)
                .ongoingMeetings(ongoingMeetings)
                .totalGroups(totalGroups)
                .totalGroupMembers(totalGroupMembers)
                .build();
    }

    @Override
    public PageResponse<MeetingVO> pageMeetings(int current, int size, String keyword, String status) {
        Page<Meeting> page = new Page<>(current, size);
        QueryWrapper<Meeting> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String key = keyword.trim();
            wrapper.and(w -> w.like("topic", key).or().like("meeting_number", key));
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("created_at");

        Page<Meeting> meetingPage = meetingMapper.selectPage(page, wrapper);
        List<Meeting> records = meetingPage.getRecords();

        Map<Long, String> hostNames = loadUsernames(records.stream()
                .map(Meeting::getHostUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()));

        Map<Long, MeetingSummary> summaries = loadLatestSummaries(records.stream()
                .map(Meeting::getId)
                .collect(Collectors.toSet()));

        List<MeetingVO> voList = records.stream()
                .map(meeting -> toMeetingVO(meeting, hostNames.get(meeting.getHostUserId()),
                        summaries.get(meeting.getId())))
                .collect(Collectors.toList());

        return PageResponse.of(current, size, meetingPage.getTotal(), voList);
    }

    @Override
    public MeetingSummaryVO getMeetingSummary(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || Boolean.TRUE.equals(meeting.getDeleted())) {
            throw new NotFoundException(404, "会议不存在");
        }
        MeetingSummaryVO success = summaryService.getLatestSuccess(meetingId);
        return success != null ? success : summaryService.getLatest(meetingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMeeting(Long meetingId, Long adminUserId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || Boolean.TRUE.equals(meeting.getDeleted())) {
            throw new NotFoundException(404, "会议不存在");
        }

        boolean active = MeetingStatus.WAITING.getCode().equals(meeting.getStatus())
                || MeetingStatus.ONGOING.getCode().equals(meeting.getStatus());

        if (active) {
            meeting.setStatus(MeetingStatus.CANCELLED.getCode());
            meeting.setActualEndTime(LocalDateTime.now());

            // 更新已加入参与者的离会状态
            List<MeetingParticipant> participants = participantMapper.findByMeetingIdAndStatus(
                    meetingId, ParticipantStatus.JOINED.getCode());
            for (MeetingParticipant participant : participants) {
                participant.setJoinStatus(ParticipantStatus.LEFT.getCode());
                participant.setLeaveTime(LocalDateTime.now());
                participantMapper.updateById(participant);
            }

            // 通知会议内成员会议已结束
            try {
                Map<String, Object> controlContent = new HashMap<>();
                controlContent.put("action", "END_MEETING");
                controlContent.put("type", "CONTROL");
                controlContent.put("meetingId", meetingId.toString());
                controlContent.put("senderId", adminUserId != null ? adminUserId.toString() : "0");
                controlContent.put("senderName", "系统");
                controlContent.put("timestamp", System.currentTimeMillis());

                MessageSendDto endMeetingMsg = new MessageSendDto();
                endMeetingMsg.setMeetingId(meetingId);
                endMeetingMsg.setSenderId(adminUserId);
                endMeetingMsg.setSenderName("系统");
                endMeetingMsg.setMessageType("CONTROL");
                endMeetingMsg.setContent(JsonUtils.toJson(controlContent));
                endMeetingMsg.setTargetScope(MessageSend2TypeEnum.GROUP.getCode());
                endMeetingMsg.setSentAt(LocalDateTime.now());
                channelContextUtils.sendMessage(endMeetingMsg);
            } catch (Exception e) {
                log.warn("管理端删除会议时发送结束通知失败 - 会议ID: {}", meetingId, e);
            }

            // 清理内存房间与 Redis 缓存
            channelContextUtils.clearMeetingRoom(meetingId.toString());
            redisUtils.delete("meeting:" + meetingId);
            redisUtils.delete("meeting:host:" + meetingId);
        }

        meeting.setDeleted(true);
        meetingMapper.updateById(meeting);

        log.info("管理端删除会议 - 会议ID: {}, 操作人: {}", meetingId, adminUserId);
    }

    @Override
    public PageResponse<AdminUserVO> pageUsers(int current, int size, String keyword, Boolean enabled) {
        Page<User> page = new Page<>(current, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String key = keyword.trim();
            wrapper.and(w -> w.like("username", key).or().like("mobile", key).or().like("email", key));
        }
        if (enabled != null) {
            wrapper.eq("status", enabled);
        }
        wrapper.orderByDesc("created_at");

        Page<User> userPage = userMapper.selectPage(page, wrapper);
        List<AdminUserVO> voList = userPage.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());

        return PageResponse.of(current, size, userPage.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Boolean enabled) {
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new NotFoundException(404, "用户不存在");
        }
        user.setEnabled(enabled);
        userMapper.updateById(user);

        if (Boolean.FALSE.equals(enabled)) {
            // 禁用后使该用户已签发的 token 失效
            jwtConfig.invalidateAllUserTokens(user.getUsername());
        }

        log.info("管理端更新用户状态 - 用户ID: {}, enabled: {}", userId, enabled);
    }

    @Override
    public PageResponse<AdminGroupVO> pageGroups(int current, int size, String keyword) {
        Page<ImGroup> page = new Page<>(current, size);
        QueryWrapper<ImGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String key = keyword.trim();
            wrapper.and(w -> w.like("group_name", key).or().like("group_no", key));
        }
        wrapper.orderByDesc("created_at");

        Page<ImGroup> groupPage = groupMapper.selectPage(page, wrapper);
        List<ImGroup> records = groupPage.getRecords();

        Set<Long> ownerIds = records.stream()
                .map(ImGroup::getOwnerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> ownerNames = loadUsernames(ownerIds);

        Set<Long> groupIds = records.stream().map(ImGroup::getId).collect(Collectors.toSet());
        Map<Long, Long> memberCounts = loadMemberCounts(groupIds);

        List<AdminGroupVO> voList = records.stream()
                .map(group -> toAdminGroupVO(group,
                        ownerNames.get(group.getOwnerId()),
                        memberCounts.getOrDefault(group.getId(), 0L)))
                .collect(Collectors.toList());

        return PageResponse.of(current, size, groupPage.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId) {
        ImGroup group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getDeleted())) {
            throw new NotFoundException(404, "群聊不存在");
        }

        group.setDeleted(true);
        groupMapper.updateById(group);

        // 移除群成员关系与会话状态
        groupMemberMapper.delete(new QueryWrapper<ImGroupMember>().eq("group_id", groupId));
        chatStateMapper.delete(new QueryWrapper<ImChatState>()
                .eq("conversation_type", "GROUP")
                .eq("target_id", groupId));

        log.info("管理端删除群聊 - 群ID: {}, 操作人: {}", groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroupStatus(Long groupId, boolean banned) {
        ImGroup group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getDeleted())) {
            throw new NotFoundException(404, "群聊不存在");
        }

        group.setStatus(banned ? "banned" : "normal");
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.updateById(group);
        log.info("管理端{}群聊 - 群ID: {}", banned ? "封禁" : "解封", groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long adminUserId, Long groupId) {
        ImGroup group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getDeleted())) {
            throw new NotFoundException(404, "群聊不存在");
        }

        long memberCount = groupMemberMapper.selectCount(new QueryWrapper<ImGroupMember>()
                .eq("group_id", groupId)
                .eq("user_id", adminUserId)
                .eq("deleted", false));
        if (memberCount > 0) {
            return;
        }

        ImGroupMember member = ImGroupMember.builder()
                .groupId(groupId)
                .userId(adminUserId)
                .roleType("member")
                .joinTime(LocalDateTime.now())
                .build();
        groupMemberMapper.insert(member);
        log.info("管理员直接加入群聊 - 群ID: {}, 管理员: {}", groupId, adminUserId);
    }

    @Override
    public PageResponse<GroupAppealVO> pageGroupAppeals(int current, int size, String status) {
        Page<ImGroupAppeal> page = new Page<>(current, size);
        QueryWrapper<ImGroupAppeal> wrapper = new QueryWrapper<ImGroupAppeal>()
                .eq("deleted", false);
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("id");

        Page<ImGroupAppeal> appealPage = groupAppealMapper.selectPage(page, wrapper);
        List<ImGroupAppeal> records = appealPage.getRecords();
        if (records.isEmpty()) {
            return PageResponse.of(current, size, appealPage.getTotal(), Collections.emptyList());
        }

        Set<Long> groupIds = new HashSet<>();
        Set<Long> ownerIds = new HashSet<>();
        for (ImGroupAppeal appeal : records) {
            groupIds.add(appeal.getGroupId());
            ownerIds.add(appeal.getOwnerId());
        }

        Map<Long, ImGroup> groupMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            for (ImGroup group : groupMapper.selectBatchIds(groupIds)) {
                groupMap.put(group.getId(), group);
            }
        }
        Map<Long, User> ownerMap = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(ownerIds)) {
                ownerMap.put(user.getId(), user);
            }
        }

        List<GroupAppealVO> voList = records.stream().map(appeal -> {
            ImGroup group = groupMap.get(appeal.getGroupId());
            User owner = ownerMap.get(appeal.getOwnerId());
            return GroupAppealVO.builder()
                    .id(appeal.getId())
                    .groupId(appeal.getGroupId())
                    .groupName(group != null ? group.getGroupName() : "群聊已删除")
                    .groupNo(group != null ? group.getGroupNo() : null)
                    .ownerId(appeal.getOwnerId())
                    .ownerName(owner != null ? owner.getUsername() : "未知用户")
                    .reason(appeal.getReason())
                    .status(appeal.getStatus())
                    .adminRemark(appeal.getAdminRemark())
                    .createdAt(appeal.getCreatedAt())
                    .updatedAt(appeal.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.of(current, size, appealPage.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewGroupAppeal(Long appealId, boolean approve, String adminRemark) {
        ImGroupAppeal appeal = groupAppealMapper.selectById(appealId);
        if (appeal == null || Boolean.TRUE.equals(appeal.getDeleted())) {
            throw new NotFoundException(404, "申诉记录不存在");
        }
        if (!"PENDING".equals(appeal.getStatus())) {
            throw new BusinessException(400, "该申诉已处理，请勿重复操作");
        }

        appeal.setStatus(approve ? "APPROVED" : "REJECTED");
        appeal.setAdminRemark(adminRemark);
        appeal.setUpdatedAt(LocalDateTime.now());
        groupAppealMapper.updateById(appeal);

        if (approve) {
            ImGroup group = groupMapper.selectById(appeal.getGroupId());
            if (group != null && !Boolean.TRUE.equals(group.getDeleted())) {
                group.setStatus("normal");
                group.setUpdatedAt(LocalDateTime.now());
                groupMapper.updateById(group);
            }
        }
        log.info("管理端审核群聊申诉 - 申诉ID: {}, 群ID: {}, 结果: {}",
                appealId, appeal.getGroupId(), approve ? "通过并解封" : "驳回");
    }

    private Map<Long, String> loadUsernames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> result = new HashMap<>();
        for (User user : users) {
            result.put(user.getId(), user.getUsername());
        }
        return result;
    }

    private Map<Long, Long> loadMemberCounts(Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ImGroupMember> members = groupMemberMapper.selectList(new QueryWrapper<ImGroupMember>()
                .eq("deleted", false)
                .in("group_id", groupIds));
        return members.stream()
                .filter(member -> member.getGroupId() != null)
                .collect(Collectors.groupingBy(ImGroupMember::getGroupId, Collectors.counting()));
    }

    private Map<Long, MeetingSummary> loadLatestSummaries(Set<Long> meetingIds) {
        if (meetingIds == null || meetingIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MeetingSummary> summaries = summaryMapper.selectList(new QueryWrapper<MeetingSummary>()
                .eq("deleted", false)
                .in("meeting_id", meetingIds)
                .orderByDesc("id"));
        Map<Long, MeetingSummary> result = new HashMap<>();
        for (MeetingSummary summary : summaries) {
            MeetingSummary existing = result.get(summary.getMeetingId());
            if (existing == null) {
                result.put(summary.getMeetingId(), summary);
            } else if (!"success".equals(existing.getStatus()) && "success".equals(summary.getStatus())) {
                // 优先展示已生成成功的纪要
                result.put(summary.getMeetingId(), summary);
            }
        }
        return result;
    }

    private MeetingVO toMeetingVO(Meeting meeting, String hostUsername, MeetingSummary summary) {
        MeetingVO vo = new MeetingVO();
        BeanUtils.copyProperties(meeting, vo);
        vo.setHostUsername(hostUsername);
        vo.setHasPassword(meeting.getPassword() != null && !meeting.getPassword().isEmpty());
        if (summary != null) {
            vo.setSummaryId(summary.getId());
            vo.setSummaryStatus(summary.getStatus());
        }
        return vo;
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private AdminGroupVO toAdminGroupVO(ImGroup group, String ownerName, Long memberCount) {
        AdminGroupVO vo = new AdminGroupVO();
        BeanUtils.copyProperties(group, vo);
        vo.setOwnerName(ownerName);
        vo.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
        return vo;
    }
}
