package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.model.dto.MeetingAiSettingsDTO;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.entity.MeetingTranslation;
import cn.cxdproject.coder.model.entity.User;
import cn.cxdproject.coder.model.vo.MeetingAiSettingsVO;
import cn.cxdproject.coder.model.vo.MeetingSummaryVO;
import cn.cxdproject.coder.service.MeetingService;
import cn.cxdproject.coder.service.ai.MeetingAiSettingsService;
import cn.cxdproject.coder.service.ai.MeetingSummaryService;
import cn.cxdproject.coder.service.ai.RealtimeTranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 会议 AI 控制器：实时翻译与智能总结
 *
 * @author heathcetide
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/ai/meeting")
public class MeetingAiController {

    @Resource
    private MeetingAiSettingsService settingsService;

    @Resource
    private RealtimeTranslationService translationService;

    @Resource
    private MeetingSummaryService summaryService;

    @Resource
    private MeetingService meetingService;

    /**
     * 获取会议 AI 配置
     */
    @GetMapping("/{meetingId}/settings")
    public ApiResponse<MeetingAiSettingsVO> getSettings(@PathVariable Long meetingId) {
        if (currentUser() == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(settingsService.toVO(settingsService.getOrDefault(meetingId)));
    }

    /**
     * 更新会议 AI 配置（仅主持人）
     */
    @PutMapping("/{meetingId}/settings")
    public ApiResponse<MeetingAiSettingsVO> updateSettings(@PathVariable Long meetingId,
                                                           @Valid @RequestBody MeetingAiSettingsDTO dto) {
        User user = currentUser();
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        Meeting meeting = requireMeeting(meetingId);
        if (!meeting.getHostUserId().equals(user.getId())) {
            throw new BusinessException(400, "只有主持人可以修改 AI 配置");
        }
        return ApiResponse.success(settingsService.toVO(settingsService.saveSettings(meetingId, dto)));
    }

    /**
     * 查询会议实时翻译记录
     */
    @GetMapping("/{meetingId}/translations")
    public ApiResponse<List<MeetingTranslation>> getTranslations(@PathVariable Long meetingId,
                                                                 @RequestParam(required = false) String targetLanguage) {
        if (currentUser() == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(translationService.listByMeeting(meetingId, targetLanguage));
    }

    /**
     * 查询会议最近一次总结
     */
    @GetMapping("/{meetingId}/summary")
    public ApiResponse<MeetingSummaryVO> getSummary(@PathVariable Long meetingId) {
        if (currentUser() == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(summaryService.getLatest(meetingId));
    }

    /**
     * 手动触发生成会议总结（仅主持人）
     */
    @PostMapping("/{meetingId}/summary")
    public ApiResponse<MeetingSummaryVO> triggerSummary(@PathVariable Long meetingId) {
        User user = currentUser();
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(summaryService.trigger(meetingId, user.getId(), "manual"));
    }

    /**
     * 基于已有总结重新生成（仅主持人）
     */
    @PostMapping("/summary/{summaryId}/regenerate")
    public ApiResponse<MeetingSummaryVO> regenerateSummary(@PathVariable Long summaryId) {
        User user = currentUser();
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(summaryService.regenerate(summaryId, user.getId()));
    }

    private User currentUser() {
        return AuthContext.getCurrentUser();
    }

    private Meeting requireMeeting(Long meetingId) {
        Meeting meeting = meetingService.getById(meetingId);
        if (meeting == null || Boolean.TRUE.equals(meeting.getDeleted())) {
            throw new NotFoundException(404, "会议不存在");
        }
        return meeting;
    }
}
