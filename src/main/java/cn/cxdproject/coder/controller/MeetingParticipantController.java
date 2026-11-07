package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.dto.InviteParticipantDTO;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.MeetingParticipantVO;
import cn.cxdproject.coder.service.MeetingParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 会议参与者控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/meeting/{meetingId}/participants")
@Validated
public class MeetingParticipantController {

    private final MeetingParticipantService participantService;

    public MeetingParticipantController(MeetingParticipantService participantService) {
        this.participantService = participantService;
    }

    /**
     * 获取参与者列表
     */
    @GetMapping
    public ApiResponse<List<MeetingParticipantVO>> getParticipants(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        List<MeetingParticipantVO> participants = participantService.getParticipants(meetingId);
        return ApiResponse.success(participants);
    }

    /**
     * 邀请参与者
     */
    @PostMapping
    public ApiResponse<MeetingParticipantVO> inviteParticipant(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId,
            @Valid @RequestBody InviteParticipantDTO dto) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingParticipantVO participant = participantService.inviteParticipant(
                currentUser.getId(), meetingId, dto);
        return ApiResponse.success(participant);
    }

    /**
     * 移除参与者
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeParticipant(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId,
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        participantService.removeParticipant(currentUser.getId(), meetingId, userId);
        return ApiResponse.success();
    }

    /**
     * 更新参与者角色
     */
    @PutMapping("/{userId}/role")
    public ApiResponse<MeetingParticipantVO> updateParticipantRole(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId,
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "角色不能为空") String role) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingParticipantVO participant = participantService.updateParticipantRole(
                currentUser.getId(), meetingId, userId, role);
        return ApiResponse.success(participant);
    }

    /**
     * 静音/取消静音参与者
     */
    @PutMapping("/{userId}/mute")
    public ApiResponse<MeetingParticipantVO> muteParticipant(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId,
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "静音状态不能为空") Boolean muted) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingParticipantVO participant = participantService.muteParticipant(
                currentUser.getId(), meetingId, userId, muted);
        return ApiResponse.success(participant);
    }
}
