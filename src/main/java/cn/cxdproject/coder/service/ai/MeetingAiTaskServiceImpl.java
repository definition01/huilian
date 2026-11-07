package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.mapper.MeetingAiTaskMapper;
import cn.cxdproject.coder.model.entity.MeetingAiTask;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 会议 AI 异步任务服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingAiTaskServiceImpl implements MeetingAiTaskService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    @Resource
    private MeetingAiTaskMapper taskMapper;

    @Override
    public MeetingAiTask findActive(Long meetingId, String taskType) {
        return taskMapper.selectOne(new LambdaQueryWrapper<MeetingAiTask>()
                .eq(MeetingAiTask::getMeetingId, meetingId)
                .eq(MeetingAiTask::getTaskType, taskType)
                .in(MeetingAiTask::getStatus, Arrays.asList(STATUS_PENDING, STATUS_RUNNING))
                .orderByDesc(MeetingAiTask::getId)
                .last("LIMIT 1"));
    }

    @Override
    public MeetingAiTask createIfAbsent(Long meetingId, String taskType, String payload) {
        MeetingAiTask existing = findActive(meetingId, taskType);
        if (existing != null) {
            log.info("AI任务已存在，跳过创建 - 会议ID: {}, 类型: {}, 任务ID: {}",
                    meetingId, taskType, existing.getId());
            return existing;
        }

        MeetingAiTask task = MeetingAiTask.builder()
                .meetingId(meetingId)
                .taskType(taskType)
                .status(STATUS_PENDING)
                .retryCount(0)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        taskMapper.insert(task);
        return task;
    }

    @Override
    public void markRunning(Long taskId) {
        updateStatus(taskId, STATUS_RUNNING, null, null);
    }

    @Override
    public void markSuccess(Long taskId, Long resultRefId) {
        updateStatus(taskId, STATUS_SUCCESS, resultRefId, null);
    }

    @Override
    public void markFailed(Long taskId, String errorMessage) {
        updateStatus(taskId, STATUS_FAILED, null, truncate(errorMessage));
    }

    private void updateStatus(Long taskId, String status, Long resultRefId, String errorMessage) {
        if (taskId == null) {
            return;
        }
        MeetingAiTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        if (STATUS_RUNNING.equals(status)) {
            task.setStartedAt(LocalDateTime.now());
        }
        if (STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status)) {
            task.setFinishedAt(LocalDateTime.now());
        }
        if (resultRefId != null) {
            task.setResultRefId(resultRefId);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        taskMapper.updateById(task);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
