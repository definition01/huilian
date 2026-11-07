package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.PageResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.dto.*;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.JoinMeetingVO;
import cn.cxdproject.coder.model.vo.MeetingListVO;
import cn.cxdproject.coder.model.vo.MeetingVO;
import cn.cxdproject.coder.service.MeetingService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 会议控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/meeting")
@Validated
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * 创建即时会议
     */
    @PostMapping("/instant")
    public ApiResponse<MeetingVO> createInstantMeeting(@Valid @RequestBody CreateInstantMeetingDTO dto) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingVO meeting = meetingService.createInstantMeeting(currentUser.getId(), dto);
        return ApiResponse.success(meeting);
    }

    /**
     * 创建预约会议
     */
    @PostMapping("/schedule")
    public ApiResponse<MeetingVO> createScheduledMeeting(@Valid @RequestBody CreateScheduledMeetingDTO dto) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingVO meeting = meetingService.createScheduledMeeting(currentUser.getId(), dto);
        return ApiResponse.success(meeting);
    }

    /**
     * 更新会议信息
     */
    @PutMapping("/{meetingId}")
    public ApiResponse<MeetingVO> updateMeeting(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId,
            @Valid @RequestBody UpdateMeetingDTO dto) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingVO meeting = meetingService.updateMeeting(currentUser.getId(), meetingId, dto);
        return ApiResponse.success(meeting);
    }

    /**
     * 取消会议
     */
    @DeleteMapping("/{meetingId}")
    public ApiResponse<Void> cancelMeeting(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        meetingService.cancelMeeting(currentUser.getId(), meetingId);
        return ApiResponse.success();
    }

    /**
     * 获取会议详情
     */
    @GetMapping("/{meetingId}")
    public ApiResponse<MeetingVO> getMeetingDetail(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        MeetingVO meeting = meetingService.getMeetingDetail(meetingId);
        return ApiResponse.success(meeting);
    }

    /**
     * 根据会议号获取会议详情
     */
    @GetMapping("/number/{meetingNumber}")
    public ApiResponse<MeetingVO> getMeetingByNumber(
            @PathVariable @NotNull(message = "会议号不能为空") String meetingNumber) {
        MeetingVO meeting = meetingService.getMeetingByNumber(meetingNumber);
        return ApiResponse.success(meeting);
    }

    /**
     * 获取会议列表（分页）
     */
    @GetMapping("/list")
    public PageResponse<MeetingListVO> getMeetingList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        Page<MeetingListVO> page = meetingService.getMeetingList(
                new Page<>(current, size), status);
        return PageResponse.of(
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal(),
                page.getRecords()
        );
    }

    /**
     * 获取我的会议列表
     */
    @GetMapping("/my")
    public PageResponse<MeetingListVO> getMyMeetings(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return PageResponse.of(current, size, 0L, null);
        }
        Page<MeetingListVO> page = meetingService.getMyMeetings(
                currentUser.getId(), new Page<>(current, size), status);
        return PageResponse.of(
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal(),
                page.getRecords()
        );
    }

    /**
     * 开始会议
     */
    @PostMapping("/{meetingId}/start")
    public ApiResponse<MeetingVO> startMeeting(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        MeetingVO meeting = meetingService.startMeeting(currentUser.getId(), meetingId);
        return ApiResponse.success(meeting);
    }

    /**
     * 结束会议
     */
    @PostMapping("/{meetingId}/end")
    public ApiResponse<Void> endMeeting(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        meetingService.endMeeting(currentUser.getId(), meetingId);
        return ApiResponse.success();
    }

    /**
     * 加入会议
     */
    @PostMapping("/join")
    public ApiResponse<JoinMeetingVO> joinMeeting(@Valid @RequestBody JoinMeetingDTO dto) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        JoinMeetingVO result = meetingService.joinMeeting(currentUser.getId(), dto);
        return ApiResponse.success(result);
    }

    /**
     * 离开会议
     */
    @PostMapping("/{meetingId}/leave")
    public ApiResponse<Void> leaveMeeting(
            @PathVariable @NotNull(message = "会议ID不能为空") Long meetingId) {
        User currentUser = AuthContext.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        meetingService.leaveMeeting(currentUser.getId(), meetingId);
        return ApiResponse.success();
    }
}
