package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.entity.MeetingTranscript;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 会议语音转文字记录 Service
 *
 * @author heathcetide
 */
public interface MeetingTranscriptService extends IService<MeetingTranscript> {

    /**
     * 保存ASR识别结果
     */
    void saveTranscript(MeetingTranscript transcript);

    /**
     * 获取会议的所有转录记录
     */
    List<MeetingTranscript> getTranscriptsByMeetingId(Long meetingId);

    /**
     * 获取会议的最终转录记录（仅返回isFinal=true的记录）
     */
    List<MeetingTranscript> getFinalTranscriptsByMeetingId(Long meetingId);

    /**
     * 删除会议的所有转录记录
     */
    void deleteByMeetingId(Long meetingId);
}
