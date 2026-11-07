package cn.cxdproject.coder.mapper;

import cn.cxdproject.coder.model.entity.Meeting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Meeting Mapper 接口
 *
 * @author heathcetide
 */
@Mapper
public interface MeetingMapper extends BaseMapper<Meeting> {

    /**
     * 根据会议号查询会议
     *
     * @param meetingNumber 会议号
     * @return 会议实体
     */
    Meeting findByMeetingNumber(@Param("meetingNumber") String meetingNumber);

    /**
     * 查询用户创建的会议列表
     *
     * @param userId 用户ID
     * @param status 会议状态（可选）
     * @return 会议列表
     */
    List<Meeting> findByHostUserId(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 查询用户参与的会议列表（通过参与者表关联）
     *
     * @param userId 用户ID
     * @param status 会议状态（可选）
     * @return 会议列表
     */
    List<Meeting> findByParticipantUserId(@Param("userId") Long userId, @Param("status") String status);
}
