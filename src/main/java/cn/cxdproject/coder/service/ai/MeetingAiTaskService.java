package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.model.entity.MeetingAiTask;

/**
 * 会议 AI 异步任务服务
 * 用于总结生成等耗时任务的去重、状态流转与可观测记录
 *
 * @author heathcetide
 */
public interface MeetingAiTaskService {

    /**
     * 查询同会议同类型进行中的任务（pending/running），不存在返回 null
     */
    MeetingAiTask findActive(Long meetingId, String taskType);

    /**
     * 创建任务；若同会议同类型已有 pending/running 任务则直接返回已有任务
     */
    MeetingAiTask createIfAbsent(Long meetingId, String taskType, String payload);

    void markRunning(Long taskId);

    void markSuccess(Long taskId, Long resultRefId);

    void markFailed(Long taskId, String errorMessage);
}
