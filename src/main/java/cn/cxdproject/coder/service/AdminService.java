package cn.cxdproject.coder.service;

import cn.cxdproject.coder.common.PageResponse;
import cn.cxdproject.coder.model.vo.AdminGroupVO;
import cn.cxdproject.coder.model.vo.AdminStatsVO;
import cn.cxdproject.coder.model.vo.AdminUserVO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import cn.cxdproject.coder.model.vo.MeetingSummaryVO;

/**
 * 后台管理服务
 *
 * @author heathcetide
 */
public interface AdminService {

    /**
     * 数据概览
     */
    AdminStatsVO getStats();

    /**
     * 会议分页查询
     */
    PageResponse<MeetingVO> pageMeetings(int current, int size, String keyword, String status);

    /**
     * 删除会议
     */
    void deleteMeeting(Long meetingId, Long adminUserId);

    /**
     * 查询会议纪要
     */
    MeetingSummaryVO getMeetingSummary(Long meetingId);

    /**
     * 用户分页查询
     */
    PageResponse<AdminUserVO> pageUsers(int current, int size, String keyword, Boolean enabled);

    /**
     * 启用/禁用用户
     */
    void updateUserStatus(Long userId, Boolean enabled);

    /**
     * 群聊分页查询
     */
    PageResponse<AdminGroupVO> pageGroups(int current, int size, String keyword);

    /**
     * 删除群聊
     */
    void deleteGroup(Long groupId);

    /**
     * 封禁/解封群聊
     */
    void updateGroupStatus(Long groupId, boolean banned);

    /**
     * 管理员直接进入群聊（无需群主审核）
     */
    void joinGroup(Long adminUserId, Long groupId);

    /**
     * 分页查询群聊封禁申诉
     */
    PageResponse<GroupAppealVO> pageGroupAppeals(int current, int size, String status);

    /**
     * 审核群聊申诉，通过时自动解封群聊
     */
    void reviewGroupAppeal(Long appealId, boolean approve, String adminRemark);
}
