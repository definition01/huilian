package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.common.enums.ParticipantRole;
import cn.cxdproject.coder.common.enums.ParticipantStatus;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.MeetingMapper;
import cn.cxdproject.coder.mapper.MeetingParticipantMapper;
import cn.cxdproject.coder.mapper.UserMapper;
import cn.cxdproject.coder.model.dto.InviteParticipantDTO;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.entity.MeetingParticipant;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.MeetingParticipantVO;
import cn.cxdproject.coder.service.MeetingParticipantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MeetingParticipant 服务实现类
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingParticipantServiceImpl extends ServiceImpl<MeetingParticipantMapper, MeetingParticipant>
        implements MeetingParticipantService {

    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;

    public MeetingParticipantServiceImpl(MeetingMapper meetingMapper, UserMapper userMapper) {
        this.meetingMapper = meetingMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingParticipantVO inviteParticipant(Long hostUserId, Long meetingId, InviteParticipantDTO dto) {
        // 验证会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限（只有主持人可以邀请）
        if (!meeting.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(400, "只有主持人可以邀请参与者");
        }

        // 验证用户是否存在
        User user = userMapper.selectById(dto.getUserId());
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new NotFoundException(404, "用户不存在");
        }

        // 检查是否已是参与者
        MeetingParticipant existing = baseMapper.findByMeetingIdAndUserId(meetingId, dto.getUserId());
        if (existing != null) {
            throw new BusinessException(400, "该用户已在会议中");
        }

        // 检查人数限制
        Integer currentCount = baseMapper.countByMeetingIdAndStatus(
                meetingId, ParticipantStatus.JOINED.getCode());
        if (currentCount >= meeting.getMaxParticipants()) {
            throw new BusinessException(400, "会议人数已达上限");
        }

        // 验证角色
        String role = dto.getRole();
        if (role == null || role.isEmpty()) {
            role = ParticipantRole.PARTICIPANT.getCode();
        } else {
            // 验证角色是否合法
            try {
                ParticipantRole.fromCode(role);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(400, "无效的角色");
            }
            // 不能直接邀请为主持人
            if (ParticipantRole.HOST.getCode().equals(role)) {
                throw new BusinessException(400, "不能邀请为主持人");
            }
        }

        // 创建参与者记录
        MeetingParticipant participant = MeetingParticipant.builder()
                .roomId(meetingId)
                .userId(dto.getUserId())
                .displayName(user.getUsername()) // 使用用户名作为显示名
                .roleType(role)
                .joinStatus(ParticipantStatus.INVITED.getCode())
                .isMicrophoneOn(!meeting.getMuteOnEntry()) // 注意：逻辑相反，麦克风开启状态
                .isCameraOn(false)
                .build();

        baseMapper.insert(participant);

        log.info("邀请参与者成功 - 会议ID: {}, 参与者ID: {}, 角色: {}", meetingId, dto.getUserId(), role);

        return toParticipantVO(participant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeParticipant(Long hostUserId, Long meetingId, Long participantId) {
        // 验证会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限（只有主持人和联席主持人可以移除参与者）
        MeetingParticipant operator = baseMapper.findByMeetingIdAndUserId(meetingId, hostUserId);
        if (operator == null) {
            throw new BusinessException(400, "您不在该会议中");
        }
        if (!ParticipantRole.HOST.getCode().equals(operator.getRoleType()) &&
                !ParticipantRole.CO_HOST.getCode().equals(operator.getRoleType())) {
            throw new BusinessException(400, "只有主持人和联席主持人可以移除参与者");
        }

        // 不能移除主持人
        if (participantId.equals(meeting.getHostUserId())) {
            throw new BusinessException(400, "不能移除主持人");
        }

        // 查询参与者
        MeetingParticipant participant = baseMapper.findByMeetingIdAndUserId(meetingId, participantId);
        if (participant == null) {
            throw new NotFoundException(404, "参与者不存在");
        }

        // 逻辑删除
        participant.setDeleted(true);
        participant.setJoinStatus(ParticipantStatus.LEFT.getCode());
        participant.setLeaveTime(LocalDateTime.now());
        baseMapper.updateById(participant);

        log.info("移除参与者成功 - 会议ID: {}, 参与者ID: {}, 操作者ID: {}", meetingId, participantId, hostUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingParticipantVO updateParticipantRole(Long hostUserId, Long meetingId, Long participantId, String role) {
        // 验证会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限（只有主持人可以更新角色）
        if (!meeting.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(400, "只有主持人可以更新参与者角色");
        }

        // 验证角色
        try {
            ParticipantRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "无效的角色");
        }

        // 不能设置为主持人
        if (ParticipantRole.HOST.getCode().equals(role)) {
            throw new BusinessException(400, "不能设置为主持人");
        }

        // 不能修改主持人自己的角色
        if (participantId.equals(hostUserId)) {
            throw new BusinessException(400, "不能修改自己的角色");
        }

        // 查询参与者
        MeetingParticipant participant = baseMapper.findByMeetingIdAndUserId(meetingId, participantId);
        if (participant == null) {
            throw new NotFoundException(404, "参与者不存在");
        }

        // 更新角色
        participant.setRoleType(role);
        baseMapper.updateById(participant);

        log.info("更新参与者角色成功 - 会议ID: {}, 参与者ID: {}, 新角色: {}", meetingId, participantId, role);

        return toParticipantVO(participant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingParticipantVO muteParticipant(Long operatorUserId, Long meetingId, Long participantId, Boolean muted) {
        // 验证会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted()) {
            throw new NotFoundException(404, "会议不存在");
        }

        // 验证权限（主持人和联席主持人可以静音任何人，普通参与者只能静音自己）
        MeetingParticipant operator = baseMapper.findByMeetingIdAndUserId(meetingId, operatorUserId);
        if (operator == null) {
            throw new BusinessException(400, "您不在该会议中");
        }

        if (!operatorUserId.equals(participantId)) {
            // 静音他人需要主持人或联席主持人权限
            if (!ParticipantRole.HOST.getCode().equals(operator.getRoleType()) &&
                    !ParticipantRole.CO_HOST.getCode().equals(operator.getRoleType())) {
                throw new BusinessException(400, "只有主持人和联席主持人可以静音他人");
            }
        }

        // 查询参与者
        MeetingParticipant participant = baseMapper.findByMeetingIdAndUserId(meetingId, participantId);
        if (participant == null) {
            throw new NotFoundException(404, "参与者不存在");
        }

        // 更新麦克风状态（注意：muted参数表示是否静音，而isMicrophoneOn表示麦克风是否开启，逻辑相反）
        participant.setIsMicrophoneOn(!muted);
        baseMapper.updateById(participant);

        log.info("更新参与者静音状态 - 会议ID: {}, 参与者ID: {}, 静音: {}, 操作者ID: {}",
                meetingId, participantId, muted, operatorUserId);

        return toParticipantVO(participant);
    }

    @Override
    public List<MeetingParticipantVO> getParticipants(Long meetingId) {
        List<MeetingParticipant> participants = baseMapper.findByMeetingId(meetingId);
        return participants.stream()
                .map(this::toParticipantVO)
                .collect(Collectors.toList());
    }

    @Override
    public MeetingParticipant getParticipant(Long meetingId, Long userId) {
        return baseMapper.findByMeetingIdAndUserId(meetingId, userId);
    }

    @Override
    public MeetingParticipantVO toParticipantVO(MeetingParticipant participant) {
        if (participant == null) {
            return null;
        }

        // 查询用户信息
        User user = userMapper.selectById(participant.getUserId());

        return MeetingParticipantVO.builder()
                .id(participant.getId())
                .userId(participant.getUserId())
                .username(user != null ? user.getUsername() : "未知用户")
                .avatar(user != null ? user.getAvatar() : null)
                .role(participant.getRoleType())
                .joinTime(participant.getJoinTime())
                .leaveTime(participant.getLeaveTime())
                .status(participant.getJoinStatus())
                .isMuted(!participant.getIsMicrophoneOn()) // 转换：麦克风开启状态转为静音状态
                .isVideoOn(participant.getIsCameraOn())
                .build();
    }
}
