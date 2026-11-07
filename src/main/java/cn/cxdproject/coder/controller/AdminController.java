package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.PageResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.AdminGroupVO;
import cn.cxdproject.coder.model.vo.AdminStatsVO;
import cn.cxdproject.coder.model.vo.AdminUserVO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.vo.MeetingSummaryVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import cn.cxdproject.coder.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 后台管理控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    /**
     * 数据概览
     */
    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    /**
     * 会议分页查询
     */
    @GetMapping("/meetings")
    public PageResponse<MeetingVO> pageMeetings(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return adminService.pageMeetings(current, size, keyword, status);
    }

    /**
     * 删除会议
     */
    @DeleteMapping("/meetings/{meetingId}")
    public ApiResponse<Void> deleteMeeting(@PathVariable Long meetingId) {
        User admin = AuthContext.getCurrentUser();
        adminService.deleteMeeting(meetingId, admin != null ? admin.getId() : null);
        return ApiResponse.success();
    }

    /**
     * 查询会议纪要
     */
    @GetMapping("/meetings/{meetingId}/summary")
    public ApiResponse<MeetingSummaryVO> getMeetingSummary(@PathVariable Long meetingId) {
        return ApiResponse.success(adminService.getMeetingSummary(meetingId));
    }

    /**
     * 用户分页查询
     */
    @GetMapping("/users")
    public PageResponse<AdminUserVO> pageUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return adminService.pageUsers(current, size, keyword, enabled);
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId,
                                              @RequestParam Boolean enabled) {
        adminService.updateUserStatus(userId, enabled);
        return ApiResponse.success();
    }

    /**
     * 群聊分页查询
     */
    @GetMapping("/groups")
    public PageResponse<AdminGroupVO> pageGroups(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return adminService.pageGroups(current, size, keyword);
    }

    /**
     * 删除群聊
     */
    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long groupId) {
        adminService.deleteGroup(groupId);
        return ApiResponse.success();
    }

    /**
     * 封禁/解封群聊
     */
    @PutMapping("/groups/{groupId}/status")
    public ApiResponse<Void> updateGroupStatus(@PathVariable Long groupId,
                                               @RequestParam Boolean banned) {
        adminService.updateGroupStatus(groupId, banned);
        return ApiResponse.success();
    }

    /**
     * 管理员直接进入群聊（无需群主审核）
     */
    @PostMapping("/groups/{groupId}/join")
    public ApiResponse<Void> joinGroup(@PathVariable Long groupId) {
        adminService.joinGroup(AuthContext.getCurrentUser().getId(), groupId);
        return ApiResponse.success();
    }

    /**
     * 群聊封禁申诉分页查询
     */
    @GetMapping("/group-appeals")
    public PageResponse<GroupAppealVO> pageGroupAppeals(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        return adminService.pageGroupAppeals(current, size, status);
    }

    /**
     * 审核群聊申诉（通过时自动解封）
     */
    @PutMapping("/group-appeals/{appealId}/review")
    public ApiResponse<Void> reviewGroupAppeal(@PathVariable Long appealId,
                                               @RequestParam Boolean approve,
                                               @RequestParam(required = false) String adminRemark) {
        adminService.reviewGroupAppeal(appealId, approve, adminRemark);
        return ApiResponse.success();
    }
}
