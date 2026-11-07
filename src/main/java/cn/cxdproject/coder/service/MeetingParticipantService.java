package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.dto.InviteParticipantDTO;
import cn.cxdproject.coder.model.entity.MeetingParticipant;
import cn.cxdproject.coder.model.vo.MeetingParticipantVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * MeetingParticipant 服务接口
 *
 * @author heathcetide
 */
public interface MeetingParticipantService extends IService<MeetingParticipant> {

    /**
     * 邀请参与者
     *
     * @param hostUserId 主持人用户ID
     * @param meetingId  会议ID
     * @param dto        邀请参与者DTO
     * @return 参与者详情
     */
    MeetingParticipantVO inviteParticipant(Long hostUserId, Long meetingId, InviteParticipantDTO dto);

    /**
     * 移除参与者
     *
     * @param hostUserId    主持人用户ID
     * @param meetingId     会议ID
     * @param participantId 参与者用户ID
     */
    void removeParticipant(Long hostUserId, Long meetingId, Long participantId);

    /**
     * 更新参与者角色
     *
     * @param hostUserId    主持人用户ID
     * @param meetingId     会议ID
     * @param participantId 参与者用户ID
     * @param role          新角色
     * @return 参与者详情
     */
    MeetingParticipantVO updateParticipantRole(Long hostUserId, Long meetingId, Long participantId, String role);

    /**
     * 静音/取消静音参与者
     *
     * @param operatorUserId 操作者用户ID
     * @param meetingId      会议ID
     * @param participantId  参与者用户ID
     * @param muted          是否静音
     * @return 参与者详情
     */
    MeetingParticipantVO muteParticipant(Long operatorUserId, Long meetingId, Long participantId, Boolean muted);

    /**
     * 获取会议参与者列表
     *
     * @param meetingId 会议ID
     * @return 参与者列表
     */
    List<MeetingParticipantVO> getParticipants(Long meetingId);

    /**
     * 获取会议的参与者记录
     *
     * @param meetingId 会议ID
     * @param userId    用户ID
     * @return 参与者实体
     */
    MeetingParticipant getParticipant(Long meetingId, Long userId);

    /**
     * 将MeetingParticipant实体转换为VO
     *
     * @param participant 参与者实体
     * @return 参与者VO
     */
    MeetingParticipantVO toParticipantVO(MeetingParticipant participant);
}
