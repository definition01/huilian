package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.model.vo.MeetingSummaryVO;

/**
 * AI 会议总结服务
 *
 * @author heathcetide
 */
public interface MeetingSummaryService {

    /**
     * 查询会议最近一次总结（优先返回成功结果）
     */
    MeetingSummaryVO getLatest(Long meetingId);

    /**
     * 查询会议最近一次生成成功的总结
     */
    MeetingSummaryVO getLatestSuccess(Long meetingId);

    /**
     * 触发生成会议总结（异步执行）
     *
     * @param triggerType auto / manual
     */
    MeetingSummaryVO trigger(Long meetingId, Long operatorId, String triggerType);

    /**
     * 基于已有总结重新生成（保留原类型与语言）
     */
    MeetingSummaryVO regenerate(Long summaryId, Long operatorId);

    /**
     * 会议结束后按配置自动触发总结
     */
    void triggerAutoIfEnabled(Long meetingId);
}
