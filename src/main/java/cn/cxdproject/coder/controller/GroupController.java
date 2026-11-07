package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.dto.AddGroupMembersDTO;
import cn.cxdproject.coder.model.dto.ApplyJoinGroupDTO;
import cn.cxdproject.coder.model.dto.CreateGroupDTO;
import cn.cxdproject.coder.model.dto.GroupAppealDTO;
import cn.cxdproject.coder.model.dto.InviteGroupMembersDTO;
import cn.cxdproject.coder.model.dto.SendGroupMessageDTO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.vo.GroupJoinRequestVO;
import cn.cxdproject.coder.model.vo.GroupMemberVO;
import cn.cxdproject.coder.model.vo.GroupMessageVO;
import cn.cxdproject.coder.model.vo.GroupSearchVO;
import cn.cxdproject.coder.model.vo.GroupVO;
import cn.cxdproject.coder.model.vo.FileVO;
import cn.cxdproject.coder.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 群聊控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Resource
    private GroupService groupService;

    /**
     * 创建群聊
     */
    @PostMapping
    public ApiResponse<GroupVO> createGroup(@Valid @RequestBody CreateGroupDTO dto) {
        return ApiResponse.success(groupService.createGroup(AuthContext.getCurrentUser().getId(), dto));
    }

    /**
     * 查询我加入的群聊
     */
    @GetMapping("/list")
    public ApiResponse<List<GroupVO>> listMyGroups() {
        return ApiResponse.success(groupService.listMyGroups(AuthContext.getCurrentUser().getId()));
    }

    /**
     * 查询群聊详情
     */
    @GetMapping("/{groupId}")
    public ApiResponse<GroupVO> getGroupDetail(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroupDetail(AuthContext.getCurrentUser().getId(), groupId));
    }

    /**
     * 通过群号搜索群聊
     */
    @GetMapping("/search")
    public ApiResponse<GroupSearchVO> searchByGroupNo(@RequestParam String groupNo) {
        return ApiResponse.success(groupService.searchByGroupNo(AuthContext.getCurrentUser().getId(), groupNo));
    }

    /**
     * 通过群号申请入群（需群主审核）
     */
    @PostMapping("/apply")
    public ApiResponse<Void> applyJoin(@Valid @RequestBody ApplyJoinGroupDTO dto) {
        groupService.applyJoin(AuthContext.getCurrentUser().getId(), dto);
        return ApiResponse.success(null);
    }

    /**
     * 查询待审核的入群申请（仅群主）
     */
    @GetMapping("/{groupId}/join-requests")
    public ApiResponse<List<GroupJoinRequestVO>> listJoinRequests(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.listJoinRequests(AuthContext.getCurrentUser().getId(), groupId));
    }

    /**
     * 通过入群申请（仅群主）
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/approve")
    public ApiResponse<Void> approveJoinRequest(@PathVariable Long groupId,
                                                @PathVariable Long requestId) {
        groupService.reviewJoinRequest(AuthContext.getCurrentUser().getId(), groupId, requestId, true);
        return ApiResponse.success(null);
    }

    /**
     * 拒绝入群申请（仅群主）
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public ApiResponse<Void> rejectJoinRequest(@PathVariable Long groupId,
                                               @PathVariable Long requestId) {
        groupService.reviewJoinRequest(AuthContext.getCurrentUser().getId(), groupId, requestId, false);
        return ApiResponse.success(null);
    }

    /**
     * 查询群成员
     */
    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberVO>> listMembers(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.listMembers(AuthContext.getCurrentUser().getId(), groupId));
    }

    /**
     * 添加群成员（仅群主）
     */
    @PostMapping("/{groupId}/members")
    public ApiResponse<Void> addMembers(@PathVariable Long groupId,
                                        @Valid @RequestBody AddGroupMembersDTO dto) {
        groupService.addMembers(AuthContext.getCurrentUser().getId(), groupId, dto);
        return ApiResponse.success(null);
    }

    /**
     * 邀请好友入群（群成员均可，普通成员邀请需群主审核）
     */
    @PostMapping("/{groupId}/invite")
    public ApiResponse<Void> inviteMembers(@PathVariable Long groupId,
                                           @Valid @RequestBody InviteGroupMembersDTO dto) {
        groupService.inviteMembers(AuthContext.getCurrentUser().getId(), groupId, dto);
        return ApiResponse.success(null);
    }

    /**
     * 移除群成员（仅群主）
     */
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId,
                                          @PathVariable Long targetUserId) {
        groupService.removeMember(AuthContext.getCurrentUser().getId(), groupId, targetUserId);
        return ApiResponse.success(null);
    }

    /**
     * 解散群聊（仅群主）
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolveGroup(@PathVariable Long groupId) {
        groupService.dissolveGroup(AuthContext.getCurrentUser().getId(), groupId);
        return ApiResponse.success(null);
    }

    /**
     * 退出群聊
     */
    @PostMapping("/{groupId}/leave")
    public ApiResponse<Void> leaveGroup(@PathVariable Long groupId) {
        groupService.leaveGroup(AuthContext.getCurrentUser().getId(), groupId);
        return ApiResponse.success(null);
    }

    /**
     * 查询群消息
     */
    @GetMapping("/{groupId}/messages")
    public ApiResponse<List<GroupMessageVO>> listMessages(@PathVariable Long groupId,
                                                         @RequestParam(required = false) Long beforeId,
                                                         @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(groupService.listMessages(
                AuthContext.getCurrentUser().getId(), groupId, beforeId, limit));
    }

    /**
     * 发送群消息
     */
    @PostMapping("/{groupId}/messages")
    public ApiResponse<GroupMessageVO> sendMessage(@PathVariable Long groupId,
                                                   @Valid @RequestBody SendGroupMessageDTO dto) {
        return ApiResponse.success(groupService.sendMessage(
                AuthContext.getCurrentUser().getId(), groupId, dto));
    }

    /**
     * 上传群聊附件（图片/文件）
     */
    @PostMapping("/{groupId}/upload")
    public ApiResponse<FileVO> uploadAttachment(@PathVariable Long groupId,
                                                @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(groupService.uploadAttachment(
                AuthContext.getCurrentUser().getId(), groupId, file));
    }

    /**
     * 群主对封禁群聊提交申诉
     */
    @PostMapping("/{groupId}/appeal")
    public ApiResponse<Void> submitAppeal(@PathVariable Long groupId,
                                          @Valid @RequestBody GroupAppealDTO dto) {
        groupService.submitAppeal(AuthContext.getCurrentUser().getId(), groupId, dto);
        return ApiResponse.success(null);
    }

    /**
     * 查询群聊最新一次申诉（仅群主）
     */
    @GetMapping("/{groupId}/appeal")
    public ApiResponse<GroupAppealVO> getLatestAppeal(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.getLatestAppeal(
                AuthContext.getCurrentUser().getId(), groupId));
    }
}
