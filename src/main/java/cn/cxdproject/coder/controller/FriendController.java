package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.dto.FriendRemarkDTO;
import cn.cxdproject.coder.model.dto.SendFriendRequestDTO;
import cn.cxdproject.coder.model.dto.SendPrivateMessageDTO;
import cn.cxdproject.coder.model.vo.FriendRequestVO;
import cn.cxdproject.coder.model.vo.FriendSearchVO;
import cn.cxdproject.coder.model.vo.FriendVO;
import cn.cxdproject.coder.model.vo.PrivateMessageVO;
import cn.cxdproject.coder.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 好友与私聊
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /**
     * 按手机号或用户名搜索用户（用户名命中多个时全部返回）
     */
    @GetMapping("/search")
    public ApiResponse<List<FriendSearchVO>> searchUsers(@RequestParam String keyword) {
        return ApiResponse.success(friendService.searchUsers(
                AuthContext.getCurrentUser().getId(), keyword));
    }

    /**
     * 发送好友申请（可附带一句申请附言）
     */
    @PostMapping("/requests")
    public ApiResponse<Void> sendRequest(@Valid @RequestBody SendFriendRequestDTO dto) {
        friendService.sendRequest(AuthContext.getCurrentUser().getId(), dto);
        return ApiResponse.success(null);
    }

    /**
     * 我收到的好友申请
     */
    @GetMapping("/requests/received")
    public ApiResponse<List<FriendRequestVO>> listReceivedRequests() {
        return ApiResponse.success(friendService.listReceivedRequests(
                AuthContext.getCurrentUser().getId()));
    }

    /**
     * 我发出的好友申请
     */
    @GetMapping("/requests/sent")
    public ApiResponse<List<FriendRequestVO>> listSentRequests() {
        return ApiResponse.success(friendService.listSentRequests(
                AuthContext.getCurrentUser().getId()));
    }

    /**
     * 同意好友申请
     */
    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<Void> approveRequest(@PathVariable Long requestId) {
        friendService.reviewRequest(AuthContext.getCurrentUser().getId(), requestId, true);
        return ApiResponse.success(null);
    }

    /**
     * 拒绝好友申请
     */
    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> rejectRequest(@PathVariable Long requestId) {
        friendService.reviewRequest(AuthContext.getCurrentUser().getId(), requestId, false);
        return ApiResponse.success(null);
    }

    /**
     * 我的好友列表
     */
    @GetMapping("/list")
    public ApiResponse<List<FriendVO>> listFriends() {
        return ApiResponse.success(friendService.listFriends(
                AuthContext.getCurrentUser().getId()));
    }

    /**
     * 修改好友备注
     */
    @PutMapping("/{friendId}/remark")
    public ApiResponse<Void> updateRemark(@PathVariable Long friendId,
                                          @Valid @RequestBody FriendRemarkDTO dto) {
        friendService.updateRemark(AuthContext.getCurrentUser().getId(), friendId, dto.getRemark());
        return ApiResponse.success(null);
    }

    /**
     * 私聊历史消息
     */
    @GetMapping("/{friendId}/messages")
    public ApiResponse<List<PrivateMessageVO>> listMessages(@PathVariable Long friendId,
                                                            @RequestParam(required = false) Long beforeId,
                                                            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return ApiResponse.success(friendService.listMessages(
                AuthContext.getCurrentUser().getId(), friendId, beforeId, limit));
    }

    /**
     * 发送私聊消息
     */
    @PostMapping("/{friendId}/messages")
    public ApiResponse<PrivateMessageVO> sendMessage(@PathVariable Long friendId,
                                                     @Valid @RequestBody SendPrivateMessageDTO dto) {
        return ApiResponse.success(friendService.sendMessage(
                AuthContext.getCurrentUser().getId(), friendId, dto));
    }
}
