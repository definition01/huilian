package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.service.MeetingTranscriptService;
import cn.cxdproject.coder.service.VolcengineAsrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 会议转录控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/transcript")
public class MeetingTranscriptController {

    @Resource
    private MeetingTranscriptService transcriptService;

    @Resource
    private VolcengineAsrService asrService;

    /**
     * 获取会议的转录记录
     */
    @GetMapping("/{meetingId}")
    public ApiResponse<List<MeetingTranscript>> getTranscripts(@PathVariable Long meetingId,
                                                                  @RequestParam(defaultValue = "false") Boolean finalOnly) {
        List<MeetingTranscript> transcripts = finalOnly
                ? transcriptService.getFinalTranscriptsByMeetingId(meetingId)
                : transcriptService.getTranscriptsByMeetingId(meetingId);

        return ApiResponse.success(transcripts);
    }

    /**
     * 删除会议的转录记录
     */
    @DeleteMapping("/{meetingId}")
    public ApiResponse<Void> deleteTranscripts(@PathVariable Long meetingId) {
        transcriptService.deleteByMeetingId(meetingId);
        return ApiResponse.success(null);
    }

    /**
     * 启动ASR（前端调用）
     */
    @PostMapping("/start")
    public ApiResponse<Void> startAsr(@RequestParam Long meetingId,
                                        @RequestParam Long userId,
                                        @RequestParam String userName) {
        // resultCallback 通过 WebSocket 推送，这里只是启动ASR
        asrService.startAsr(meetingId, userId, userName, null);
        return ApiResponse.success(null);
    }

    /**
     * 停止ASR
     */
    @PostMapping("/stop")
    public ApiResponse<Void> stopAsr(@RequestParam Long meetingId,
                                       @RequestParam Long userId) {
        asrService.stopAsr(meetingId, userId);
        return ApiResponse.success(null);
    }
}
