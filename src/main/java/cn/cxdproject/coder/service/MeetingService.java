package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.dto.*;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.vo.JoinMeetingVO;
import cn.cxdproject.coder.model.vo.MeetingListVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Meeting 服务接口
 *
 * @author heathcetide
 */
public interface MeetingService extends IService<Meeting> {

    /**
     * 创建即时会议
     *
     * @param userId 用户ID
     * @param dto    创建即时会议DTO
     * @return 会议详情
     */
    MeetingVO createInstantMeeting(Long userId, CreateInstantMeetingDTO dto);

    /**
     * 创建预约会议
     *
     * @param userId 用户ID
     * @param dto    创建预约会议DTO
     * @return 会议详情
     */
    MeetingVO createScheduledMeeting(Long userId, CreateScheduledMeetingDTO dto);

    /**
     * 更新会议信息
     *
     * @param userId    用户ID
     * @param meetingId 会议ID
     * @param dto       更新会议DTO
     * @return 会议详情
     */
    MeetingVO updateMeeting(Long userId, Long meetingId, UpdateMeetingDTO dto);

    /**
     * 取消会议
     *
     * @param userId    用户ID
     * @param meetingId 会议ID
     */
    void cancelMeeting(Long userId, Long meetingId);

    /**
     * 获取会议详情
     *
     * @param meetingId 会议ID
     * @return 会议详情
     */
    MeetingVO getMeetingDetail(Long meetingId);

    /**
     * 根据会议号获取会议详情
     *
     * @param roomNo 会议号
     * @return 会议详情
     */
    MeetingVO getMeetingByNumber(String meetingNumber);

    /**
     * 获取会议列表（分页）
     *
     * @param page   分页对象
     * @param status 会议状态（可选）
     * @return 会议列表
     */
    Page<MeetingListVO> getMeetingList(Page<Meeting> page, String status);

    /**
     * 获取我的会议列表
     *
     * @param userId 用户ID
     * @param page   分页对象
     * @param status 会议状态（可选）
     * @return 会议列表
     */
    Page<MeetingListVO> getMyMeetings(Long userId, Page<Meeting> page, String status);

    /**
     * 开始会议
     *
     * @param userId    用户ID
     * @param meetingId 会议ID
     * @return 会议详情
     */
    MeetingVO startMeeting(Long userId, Long meetingId);

    /**
     * 结束会议
     *
     * @param userId    用户ID
     * @param meetingId 会议ID
     */
    void endMeeting(Long userId, Long meetingId);

    /**
     * 加入会议
     *
     * @param userId 用户ID
     * @param dto    加入会议DTO
     * @return 加入会议响应（包含Netty连接信息）
     */
    JoinMeetingVO joinMeeting(Long userId, JoinMeetingDTO dto);

    /**
     * 离开会议
     *
     * @param userId    用户ID
     * @param meetingId 会议ID
     */
    void leaveMeeting(Long userId, Long meetingId);

    /**
     * 生成唯一的会议号
     *
     * @return 9位会议号
     */
    String generateMeetingNumber();

    /**
     * 将Meeting实体转换为MeetingVO
     *
     * @param meeting 会议实体
     * @return 会议VO
     */
    MeetingVO toMeetingVO(Meeting meeting);

    /**
     * 将Meeting实体转换为MeetingListVO
     *
     * @param meeting 会议实体
     * @return 会议列表VO
     */
    MeetingListVO toMeetingListVO(Meeting meeting);
}
