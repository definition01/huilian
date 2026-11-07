package cn.cxdproject.coder.mapper;

import cn.cxdproject.coder.model.entity.MeetingParticipant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MeetingParticipant Mapper 接口
 *
 * @author heathcetide
 */
@Mapper
public interface MeetingParticipantMapper extends BaseMapper<MeetingParticipant> {

    /**
     * 查询会议的所有参与者
     *
     * @param meetingId 会议ID
     * @return 参与者列表
     */
    List<MeetingParticipant> findByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 查询会议的指定状态参与者
     *
     * @param meetingId 会议ID
     * @param status    参与者状态
     * @return 参与者列表
     */
    List<MeetingParticipant> findByMeetingIdAndStatus(@Param("meetingId") Long meetingId, @Param("status") String status);

    /**
     * 查询用户在某个会议中的参与记录
     *
     * @param meetingId 会议ID
     * @param userId    用户ID
     * @return 参与者实体
     */
    MeetingParticipant findByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * 统计会议当前参与人数
     *
     * @param meetingId 会议ID
     * @param status    参与者状态
     * @return 参与人数
     */
    Integer countByMeetingIdAndStatus(@Param("meetingId") Long meetingId, @Param("status") String status);
}
