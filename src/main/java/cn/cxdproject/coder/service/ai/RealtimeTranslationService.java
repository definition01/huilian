package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.model.entity.MeetingTranslation;

import java.util.List;

/**
 * 会议实时翻译服务
 *
 * @author heathcetide
 */
public interface RealtimeTranslationService {

    /**
     * 对一条最终 ASR 结果异步执行翻译（内部使用线程池，不阻塞 ASR 回调线程）
     */
    void translateFinalTranscript(MeetingTranscript transcript);

    /**
     * 查询会议翻译记录，targetLanguage 为空时返回全部语种
     */
    List<MeetingTranslation> listByMeeting(Long meetingId, String targetLanguage);
}
