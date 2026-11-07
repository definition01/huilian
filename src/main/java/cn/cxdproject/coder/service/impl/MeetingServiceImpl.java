package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.common.enums.MeetingStatus;
import cn.cxdproject.coder.common.enums.MeetingType;
import cn.cxdproject.coder.common.enums.ParticipantRole;
import cn.cxdproject.coder.common.enums.ParticipantStatus;
import cn.cxdproject.coder.config.JwtConfig;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.MeetingMapper;
import cn.cxdproject.coder.mapper.MeetingParticipantMapper;
import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.dto.*;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.entity.MeetingParticipant;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.JoinMeetingVO;
import cn.cxdproject.coder.model.vo.MeetingListVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import cn.cxdproject.coder.service.MeetingService;
import cn.cxdproject.coder.service.ai.MeetingSummaryService;
import cn.cxdproject.coder.utils.RedisUtils;
import cn.cxdproject.coder.utils.SnowflakeIdGenerator;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Meeting 服务实现类
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting> implements MeetingService {

    private final MeetingParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final JwtConfig jwtConfig;
    private final ChannelContextUtils channelContextUtils;
    private final MeetingSummaryService meetingSummaryService;
    private final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator();

    @Value("${meeting.netty.host:localhost}")
    private String nettyHost;

    @Value("${meeting.netty.port:8081}")
    private Integer nettyPort;

    public MeetingServiceImpl(MeetingParticipantMapper participantMapper,
                              UserMapper userMapper,
                              RedisUtils redisUtils,
                              JwtConfig jwtConfig,
                              ChannelContextUtils channelContextUtils,
                              MeetingSummaryService meetingSummaryService) {
        this.participantMapper = participantMapper;
        this.userMapper = userMapper;
        this.redisUtils = redisUtils;
        this.jwtConfig = jwtConfig;
        this.channelContextUtils = channelContextUtils;
        this.meetingSummaryService = meetingSummaryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingVO createInstantMeeting(Long userId, CreateInstantMeetingDTO dto) {
        // 生成会议号
        String meetingNumber = generateMeetingNumber();

        // 构建会议实体
        Meeting meeting = Meeting.builder()
                .meetingNumber(meetingNumber)
                .topic(dto.getTopic())
                .hostUserId(userId)
                .type(MeetingType.INSTANT.getCode())
                .password(dto.getPassword())
                .status(MeetingStatus.WAITING.getCode())
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 300)
                .description(dto.getDescription())
                .muteOnEntry(dto.getMuteOnEntry() != null ? dto.getMuteOnEntry() : false)
                .allowScreenShare(dto.getAllowScreenShare() != null ? dto.getAllowScreenShare() : true)
                .waitingRoomEnabled(dto.getWaitingRoomEnabled() != null ? dto.getWaitingRoomEnabled() : false)
                .recordEnabled(dto.getRecordEnabled() != null ? dto.getRecordEnabled() : false)
                .autoRecord(dto.getAutoRecord() != null ? dto.getAutoRecord() : false)
                .joinLink(generateJoinLink(meetingNumber))
                .build();

        // 保存会议
        save(meeting);

        // 查询主持人用户信息
        User hostUser = userMapper.selectById(userId);

        // 创建主持人参与记录
        MeetingParticipant host = MeetingParticipant.builder()
                .roomId(meeting.getId())
                .userId(userId)
                .displayName(hostUser != null ? hostUser.getUsername() : "主持人")
                .roleType(ParticipantRole.HOST.getCode())
                .joinStatus(ParticipantStatus.INVITED.getCode())
                .isMicrophoneOn(true) // 主持人默认麦克风开启
                .isCameraOn(false)
                .build();
        participantMapper.insert(host);

        log.info("创建即时会议成功 - 会议号: {}, 主持人ID: {}", meetingNumber, userId);

        return toMeetingVO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingVO createScheduledMeeting(Long userId, CreateScheduledMeetingDTO dto) {
        // 验证预约时间
        if (dto.getScheduledStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "预约时间不能早于当前时间");
        }

        // 生成会议号
        String meetingNumber = generateMeetingNumber();

        // 计算预约结束时间，未指定时长时默认 60 分钟，避免空指针
        Integer duration = dto.getDuration() != null ? dto.getDuration() : 60;
        LocalDateTime scheduledEndTime = dto.getScheduledStartTime().plusMinutes(duration);

        // 构建会议实体
        Meeting meeting = Meeting.builder()
                .meetingNumber(meetingNumber)
                .topic(dto.getTopic())
                .hostUserId(userId)
                .type(MeetingType.SCHEDULED.getCode())
                .password(dto.getPassword())
                .status(MeetingStatus.WAITING.getCode())
                .scheduledStartTime(dto.getScheduledStartTime())
                .scheduledEndTime(scheduledEndTime)
                .duration(duration)
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 300)
                .description(dto.getDescription())
                .muteOnEntry(dto.getMuteOnEntry() != null ? dto.getMuteOnEntry() : false)
                .allowScreenShare(dto.getAllowScreenShare() != null ? dto.getAllowScreenShare() : true)
                .waitingRoomEnabled(dto.getWaitingRoomEnabled() != null ? dto.getWaitingRoomEnabled() : false)
                .recordEnabled(dto.getRecordEnabled() != null ? dto.getRecordEnabled() : false)
                .autoRecord(dto.getAutoRecord() != null ? dto.getAutoRecord() : false)
                .joinLink(generateJoinLink(meetingNumber))
                .build();

        // 保存会议
        save(meeting);

        // 查询主持人用户信息
        User hostUser = userMapper.selectById(userId);

        // 创建主持人参与记录
        MeetingParticipant host = MeetingParticipant.builder()
                .roomId(meeting.getId())
                .userId(userId)
                .displayName(hostUser != null ? hostUser.getUsername() : "主持人")
                .roleType(ParticipantRole.HOST.getCode())
                .joinStatus(ParticipantStatus.INVITED.getCode())
                .isMicrophoneOn(true) // 主持人默认麦克风开启
                .isCameraOn(false)
                .build();
        participantMapper.insert(host);

        log.info("创建预约会议成功 - 会议号: {}, 预约时间: {}, 主持人ID: {}",
                meetingNumber, dto.getScheduledStartTime(), userId);

        return toMeetingVO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingVO updateMeeting(Long userId, Long meetingId, UpdateMeetingDTO dto) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限（只有主持人可以修改）
        if (!meeting.getHostUserId().equals(userId)) {
            throw new BusinessException(400, "只有主持人可以修改会议信息");
        }

        // 验证会议状态（已结束或已取消的会议不能修改）
        if (MeetingStatus.ENDED.getCode().equals(meeting.getStatus()) ||
                MeetingStatus.CANCELLED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "已结束或已取消的会议不能修改");
        }

        // 更新字段
        if (dto.getTopic() != null) {
            meeting.setTopic(dto.getTopic());
        }
        if (dto.getPassword() != null) {
            meeting.setPassword(dto.getPassword());
        }
        if (dto.getMaxParticipants() != null) {
            meeting.setMaxParticipants(dto.getMaxParticipants());
        }
        if (dto.getDescription() != null) {
            meeting.setDescription(dto.getDescription());
        }
        if (dto.getMuteOnEntry() != null) {
            meeting.setMuteOnEntry(dto.getMuteOnEntry());
        }
        if (dto.getAllowScreenShare() != null) {
            meeting.setAllowScreenShare(dto.getAllowScreenShare());
        }
        if (dto.getWaitingRoomEnabled() != null) {
            meeting.setWaitingRoomEnabled(dto.getWaitingRoomEnabled());
        }
        if (dto.getRecordEnabled() != null) {
            meeting.setRecordEnabled(dto.getRecordEnabled());
        }
        if (dto.getAutoRecord() != null) {
            meeting.setAutoRecord(dto.getAutoRecord());
        }

        // 预约会议可以修改时间
        if (MeetingType.SCHEDULED.getCode().equals(meeting.getType())) {
            if (dto.getScheduledStartTime() != null) {
                if (dto.getScheduledStartTime().isBefore(LocalDateTime.now())) {
                    throw new BusinessException(400, "预约时间不能早于当前时间");
                }
                meeting.setScheduledStartTime(dto.getScheduledStartTime());

                // 重新计算结束时间
                if (dto.getDuration() != null) {
                    meeting.setDuration(dto.getDuration());
                    meeting.setScheduledEndTime(dto.getScheduledStartTime().plusMinutes(dto.getDuration()));
                } else if (meeting.getDuration() != null) {
                    meeting.setScheduledEndTime(dto.getScheduledStartTime().plusMinutes(meeting.getDuration()));
                }
            }
        }

        updateById(meeting);

        log.info("更新会议成功 - 会议ID: {}, 操作者ID: {}", meetingId, userId);

        return toMeetingVO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMeeting(Long userId, Long meetingId) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限
        if (!meeting.getHostUserId().equals(userId)) {
            throw new BusinessException(400, "只有创建者可以取消会议");
        }

        // 验证会议状态
        if (MeetingStatus.ENDED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已结束，无法取消");
        }
        if (MeetingStatus.CANCELLED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已取消");
        }

        // 更新状态为已取消
        meeting.setStatus(MeetingStatus.CANCELLED.getCode());
        updateById(meeting);

        // 清除Redis缓存
        redisUtils.delete("meeting:" + meetingId);

        log.info("取消会议成功 - 会议ID: {}, 操作者ID: {}", meetingId, userId);
    }

    @Override
    public MeetingVO getMeetingDetail(Long meetingId) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }
        return toMeetingVO(meeting);
    }

    @Override
    public MeetingVO getMeetingByNumber(String meetingNumber) {
        Meeting meeting = baseMapper.findByMeetingNumber(meetingNumber);
        if (meeting == null) {
            throw new NotFoundException(404, "会议不存在");
        }
        return toMeetingVO(meeting);
    }

    @Override
    public Page<MeetingListVO> getMeetingList(Page<Meeting> page, String status) {
        QueryWrapper<Meeting> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");

        Page<Meeting> meetingPage = page(page, wrapper);

        Page<MeetingListVO> result = new Page<>(meetingPage.getCurrent(), meetingPage.getSize(), meetingPage.getTotal());
        result.setRecords(meetingPage.getRecords().stream()
                .map(this::toMeetingListVO)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public Page<MeetingListVO> getMyMeetings(Long userId, Page<Meeting> page, String status) {
        // 查询用户创建的会议和参与的会议
        List<Meeting> hostedMeetings = baseMapper.findByHostUserId(userId, status);
        List<Meeting> participatedMeetings = baseMapper.findByParticipantUserId(userId, status);

        // 合并去重
        Map<Long, Meeting> meetingMap = new HashMap<>();
        hostedMeetings.forEach(m -> meetingMap.put(m.getId(), m));
        participatedMeetings.forEach(m -> meetingMap.put(m.getId(), m));

        List<Meeting> allMeetings = meetingMap.values().stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .collect(Collectors.toList());

        // 手动分页
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = Math.min(start + (int) page.getSize(), allMeetings.size());
        List<Meeting> pagedMeetings = allMeetings.subList(start, end);

        Page<MeetingListVO> result = new Page<>(page.getCurrent(), page.getSize(), allMeetings.size());
        result.setRecords(pagedMeetings.stream()
                .map(this::toMeetingListVO)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingVO startMeeting(Long userId, Long meetingId) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限
        if (!meeting.getHostUserId().equals(userId)) {
            throw new BusinessException(400, "只有主持人可以开始会议");
        }

        // 验证会议状态
        if (MeetingStatus.ONGOING.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已在进行中");
        }
        if (MeetingStatus.ENDED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已结束");
        }
        if (MeetingStatus.CANCELLED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已取消");
        }

        // 更新状态和实际开始时间
        meeting.setStatus(MeetingStatus.ONGOING.getCode());
        meeting.setActualStartTime(LocalDateTime.now());
        updateById(meeting);

        // 更新主持人状态为已加入
        MeetingParticipant host = participantMapper.findByMeetingIdAndUserId(meetingId, userId);
        if (host != null) {
            host.setJoinStatus(ParticipantStatus.JOINED.getCode());
            host.setJoinTime(LocalDateTime.now());
            participantMapper.updateById(host);
        }

        // 缓存到Redis
        redisUtils.set("meeting:" + meetingId, meeting, 24 * 60 * 60); // 24小时过期
        redisUtils.set("meeting:host:" + meetingId, userId, 24 * 60 * 60); // 缓存主持人ID

        log.info("开始会议 - 会议ID: {}, 主持人ID: {}", meetingId, userId);

        return toMeetingVO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endMeeting(Long userId, Long meetingId) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限
        if (!meeting.getHostUserId().equals(userId)) {
            throw new BusinessException(400, "只有主持人可以结束会议");
        }

        // 验证会议状态
        if (MeetingStatus.ENDED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已结束");
        }
        if (MeetingStatus.CANCELLED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已取消");
        }

        // 更新状态和实际结束时间
        meeting.setStatus(MeetingStatus.ENDED.getCode());
        meeting.setActualEndTime(LocalDateTime.now());
        updateById(meeting);

        // 更新所有参与者状态为已离开
        List<MeetingParticipant> participants = participantMapper.findByMeetingIdAndStatus(
                meetingId, ParticipantStatus.JOINED.getCode());
        for (MeetingParticipant participant : participants) {
            participant.setJoinStatus(ParticipantStatus.LEFT.getCode());
            participant.setLeaveTime(LocalDateTime.now());
            participantMapper.updateById(participant);
        }

        // 发送会议结束通知
        try {
            Map<String, Object> controlContent = new HashMap<>();
            controlContent.put("action", "END_MEETING");
            controlContent.put("type", "CONTROL");
            controlContent.put("meetingId", meetingId.toString());
            controlContent.put("senderId", userId.toString());
            controlContent.put("senderName", "系统");
            controlContent.put("timestamp", System.currentTimeMillis());

            MessageSendDto endMeetingMsg = new MessageSendDto();
            endMeetingMsg.setMeetingId(meetingId);
            endMeetingMsg.setSenderId(userId);
            endMeetingMsg.setSenderName("系统");
            endMeetingMsg.setMessageType("CONTROL");
            endMeetingMsg.setContent(JsonUtils.toJson(controlContent));
            endMeetingMsg.setTargetScope(MessageSend2TypeEnum.GROUP.getCode());
            endMeetingMsg.setSentAt(LocalDateTime.now());
            channelContextUtils.sendMessage(endMeetingMsg);
            log.info("已发送会议结束通知 - 会议ID: {}", meetingId);
        } catch (Exception e) {
            log.error("发送会议结束通知失败 - 会议ID: {}", meetingId, e);
        }

        // 清空会议房间
        channelContextUtils.clearMeetingRoom(meetingId.toString());

        // 清除Redis缓存
        redisUtils.delete("meeting:" + meetingId);
        redisUtils.delete("meeting:host:" + meetingId);

        // 按配置自动生成 AI 会议总结：放到事务提交后执行，避免 AI 异常导致结束会议回滚
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    meetingSummaryService.triggerAutoIfEnabled(meetingId);
                }
            });
        } else {
            meetingSummaryService.triggerAutoIfEnabled(meetingId);
        }

        log.info("结束会议 - 会议ID: {}, 主持人ID: {}", meetingId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JoinMeetingVO joinMeeting(Long userId, JoinMeetingDTO dto) {
        // 查询会议
        Meeting meeting = baseMapper.findByMeetingNumber(dto.getMeetingNumber());
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证会议状态
        if (MeetingStatus.CANCELLED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已取消");
        }
        if (MeetingStatus.ENDED.getCode().equals(meeting.getStatus())) {
            throw new BusinessException(400, "会议已结束");
        }

        // 验证密码
        if (meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            if (dto.getPassword() == null || !dto.getPassword().equals(meeting.getPassword())) {
                throw new BusinessException(400, "会议密码错误");
            }
        }

        // 检查是否已是参与者
        MeetingParticipant existingParticipant = participantMapper.findByMeetingIdAndUserId(
                meeting.getId(), userId);

        if (existingParticipant == null) {
            // 检查人数限制
            Integer currentCount = participantMapper.countByMeetingIdAndStatus(
                    meeting.getId(), ParticipantStatus.JOINED.getCode());
            if (currentCount >= meeting.getMaxParticipants()) {
                throw new BusinessException(400, "会议人数已达上限");
            }

            // 查询用户信息
            User joinUser = userMapper.selectById(userId);

            // 创建新参与者记录
            existingParticipant = MeetingParticipant.builder()
                    .roomId(meeting.getId())
                    .userId(userId)
                    .displayName(joinUser != null ? joinUser.getUsername() : "参与者")
                    .roleType(ParticipantRole.PARTICIPANT.getCode())
                    .joinStatus(Boolean.TRUE.equals(meeting.getWaitingRoomEnabled()) ?
                            ParticipantStatus.WAITING.getCode() : ParticipantStatus.JOINED.getCode())
                    .joinTime(LocalDateTime.now())
                    .isMicrophoneOn(!Boolean.TRUE.equals(meeting.getMuteOnEntry())) // 注意：逻辑相反
                    .isCameraOn(false)
                    .build();
            participantMapper.insert(existingParticipant);
        } else {
            // 更新状态为已加入
            if (!ParticipantStatus.JOINED.getCode().equals(existingParticipant.getJoinStatus())) {
                existingParticipant.setJoinStatus(ParticipantStatus.JOINED.getCode());
                existingParticipant.setJoinTime(LocalDateTime.now());
                participantMapper.updateById(existingParticipant);
            }
        }

        // 更新用户的 meetingId
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        user.setMeetingId(meeting.getId().toString());
        userMapper.updateById(user);

        // 使用登录时的 token（从 Redis 获取）
        String authToken = (String) redisUtils.get(userId.toString());
        if (authToken == null) {
            // 如果 Redis 中没有，重新生成（兜底逻辑）
            authToken = jwtConfig.generateToken(user);
            redisUtils.set(userId.toString(), authToken, 24 * 60 * 60);
        }

        // 将用户加入到 Netty 会议房间（如果用户已经建立了 WebSocket 连接）
        try {
            channelContextUtils.addMeetingRoom(meeting.getId().toString(), userId.toString());
            log.info("用户已加入 Netty 会议房间 - 会议ID: {}, 用户ID: {}", meeting.getId(), userId);
        } catch (Exception e) {
            // 如果用户还没建立 WebSocket 连接，这里会失败，但不影响后续流程
            // 用户建立 WebSocket 连接后，ChannelContextUtils.addContext() 会自动加入会议房间
            log.debug("用户暂未建立 WebSocket 连接，将在连接建立时自动加入会议房间 - 用户ID: {}", userId);
        }

        log.info("用户加入会议 - 会议号: {}, 会议ID: {}, 用户ID: {}", dto.getMeetingNumber(), meeting.getId(), userId);

        // 构建响应
        return JoinMeetingVO.builder()
                .meeting(toMeetingVO(meeting))
                .nettyHost(nettyHost)
                .nettyPort(nettyPort)
                .authToken(authToken)
                .role(existingParticipant.getRoleType())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveMeeting(Long userId, Long meetingId) {
        Meeting meeting = getById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 查询参与者记录
        MeetingParticipant participant = participantMapper.findByMeetingIdAndUserId(meetingId, userId);
        if (participant == null) {
            throw new BusinessException(400, "您不在该会议中");
        }

        // 更新状态为已离开
        participant.setJoinStatus(ParticipantStatus.LEFT.getCode());
        participant.setLeaveTime(LocalDateTime.now());
        participantMapper.updateById(participant);

        // 清除用户的 meetingId
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setMeetingId(null);
            userMapper.updateById(user);
        }

        // 将用户从 Netty 会议房间移除
        try {
            channelContextUtils.removeFromMeetingRoom(meetingId.toString(), userId.toString());
            log.info("用户已离开 Netty 会议房间 - 会议ID: {}, 用户ID: {}", meetingId, userId);
        } catch (Exception e) {
            log.debug("用户离开 Netty 会议房间失败 - 会议ID: {}, 用户ID: {}", meetingId, userId);
        }

        log.info("用户离开会议 - 会议ID: {}, 用户ID: {}", meetingId, userId);
    }

    @Override
    public String generateMeetingNumber() {
        // 最大重试次数，避免极端情况下栈溢出
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            // 使用雪花算法生成唯一ID，取后9位作为会议号
            long id = idGenerator.nextId();
            String meetingNumber = String.valueOf(Math.abs(id) % 1000000000);

            // 确保是9位数字，不足补0
            while (meetingNumber.length() < 9) {
                meetingNumber = "0" + meetingNumber;
            }

            // 检查是否已存在
            Meeting existing = baseMapper.findByMeetingNumber(meetingNumber);
            if (existing == null) {
                return meetingNumber;
            }
            log.warn("会议号重复，正在重试 - 第{}次", i + 1);
        }
        throw new BusinessException(500, "生成会议号失败，请稍后重试");
    }

    @Override
    public MeetingVO toMeetingVO(Meeting meeting) {
        if (meeting == null) {
            return null;
        }

        // 查询主持人信息
        User host = userMapper.selectById(meeting.getHostUserId());

        // 统计当前参与人数
        Integer currentCount = participantMapper.countByMeetingIdAndStatus(
                meeting.getId(), ParticipantStatus.JOINED.getCode());

        return MeetingVO.builder()
                .id(meeting.getId())
                .meetingNumber(meeting.getMeetingNumber())
                .topic(meeting.getTopic())
                .hostUserId(meeting.getHostUserId())
                .hostUsername(host != null ? host.getUsername() : "未知")
                .type(meeting.getType())
                .hasPassword(meeting.getPassword() != null && !meeting.getPassword().isEmpty())
                .status(meeting.getStatus())
                .scheduledStartTime(meeting.getScheduledStartTime())
                .scheduledEndTime(meeting.getScheduledEndTime())
                .actualStartTime(meeting.getActualStartTime())
                .actualEndTime(meeting.getActualEndTime())
                .duration(meeting.getDuration())
                .maxParticipants(meeting.getMaxParticipants())
                .currentParticipants(currentCount)
                .joinLink(meeting.getJoinLink())
                .description(meeting.getDescription())
                .muteOnEntry(meeting.getMuteOnEntry())
                .allowScreenShare(meeting.getAllowScreenShare())
                .waitingRoomEnabled(meeting.getWaitingRoomEnabled())
                .recordEnabled(meeting.getRecordEnabled())
                .autoRecord(meeting.getAutoRecord())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    @Override
    public MeetingListVO toMeetingListVO(Meeting meeting) {
        if (meeting == null) {
            return null;
        }

        // 查询主持人信息
        User host = userMapper.selectById(meeting.getHostUserId());

        // 统计当前参与人数
        Integer currentCount = participantMapper.countByMeetingIdAndStatus(
                meeting.getId(), ParticipantStatus.JOINED.getCode());

        return MeetingListVO.builder()
                .id(meeting.getId())
                .meetingNumber(meeting.getMeetingNumber())
                .topic(meeting.getTopic())
                .hostUserId(meeting.getHostUserId())
                .hostUsername(host != null ? host.getUsername() : "未知")
                .type(meeting.getType())
                .status(meeting.getStatus())
                .scheduledStartTime(meeting.getScheduledStartTime())
                .actualStartTime(meeting.getActualStartTime())
                .duration(meeting.getDuration())
                .currentParticipants(currentCount)
                .maxParticipants(meeting.getMaxParticipants())
                .createdAt(meeting.getCreatedAt())
                .build();
    }

    /**
     * 生成入会链接
     */
    private String generateJoinLink(String meetingNumber) {
        // 这里可以根据实际需求生成链接，例如前端页面URL
        return "https://meeting.example.com/join/" + meetingNumber;
    }
}
