package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.config.MeetingAiProperties;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.exception.NotFoundException;
import cn.cxdproject.coder.mapper.MeetingMapper;
import cn.cxdproject.coder.mapper.MeetingSummaryMapper;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.Meeting;
import cn.cxdproject.coder.model.entity.MeetingAiSettings;
import cn.cxdproject.coder.model.entity.MeetingAiTask;
import cn.cxdproject.coder.model.entity.MeetingSummary;
import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.model.vo.MeetingSummaryVO;
import cn.cxdproject.coder.service.MeetingTranscriptService;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 会议总结服务实现
 * 会议结束后自动触发，也支持手动触发与重新生成；长会采用 map-reduce 分段提炼
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingSummaryServiceImpl implements MeetingSummaryService {

    private static final String TASK_TYPE_SUMMARY = "SUMMARY";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_GENERATING = "generating";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是专业的会议纪要助理。请基于会议逐字稿生成结构化会议纪要。"
                    + "严格只输出一个 JSON 对象，不要输出解释、不要使用 Markdown 代码块。";

    private static final String CHUNK_SYSTEM_PROMPT =
            "你是会议内容提炼助手。请把给定的会议逐字稿片段压缩为要点清单，"
                    + "保留结论、决策、待办、负责人与时间要求，只输出纯文本要点，不要输出 JSON。";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MeetingAiProperties properties;

    @Resource
    private DashScopeClient dashScopeClient;

    @Resource
    private MeetingAiSettingsService settingsService;

    @Resource
    private MeetingAiTaskService taskService;

    @Resource
    private MeetingSummaryMapper summaryMapper;

    @Resource
    private MeetingMapper meetingMapper;

    @Resource
    private MeetingTranscriptService transcriptService;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public MeetingSummaryVO getLatest(Long meetingId) {
        // 直接返回最近一条记录（可能为生成中），前端据此展示进度并轮询
        MeetingSummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<MeetingSummary>()
                .eq(MeetingSummary::getMeetingId, meetingId)
                .orderByDesc(MeetingSummary::getId)
                .last("LIMIT 1"));
        return summary != null ? toVO(summary) : null;
    }

    @Override
    public MeetingSummaryVO getLatestSuccess(Long meetingId) {
        MeetingSummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<MeetingSummary>()
                .eq(MeetingSummary::getMeetingId, meetingId)
                .eq(MeetingSummary::getStatus, STATUS_SUCCESS)
                .orderByDesc(MeetingSummary::getId)
                .last("LIMIT 1"));
        return summary != null ? toVO(summary) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingSummaryVO trigger(Long meetingId, Long operatorId, String triggerType) {
        Meeting meeting = requireMeeting(meetingId);
        if (operatorId != null && !operatorId.equals(meeting.getHostUserId())) {
            throw new BusinessException(400, "只有主持人可以生成会议总结");
        }
        if (!properties.isEnabled()) {
            throw new BusinessException(400, "AI 功能未启用");
        }

        MeetingAiSettings settings = settingsService.getOrDefault(meetingId);
        if (!Boolean.TRUE.equals(settings.getSummaryEnabled())) {
            throw new BusinessException(400, "该会议未启用 AI 总结");
        }

        // 已有进行中的任务时直接返回当前总结记录，避免重复生成
        MeetingAiTask activeTask = taskService.findActive(meetingId, TASK_TYPE_SUMMARY);
        if (activeTask != null) {
            MeetingSummary inProgress = findLatestInProgress(meetingId);
            if (inProgress != null) {
                log.info("会议总结生成中，返回进行中的记录 - 会议ID: {}, 总结ID: {}", meetingId, inProgress.getId());
                return toVO(inProgress);
            }
        }

        String language = settings.getSummaryLanguage() != null ? settings.getSummaryLanguage() : "zh";
        MeetingSummary summary = MeetingSummary.builder()
                .meetingId(meetingId)
                .summaryType("full")
                .language(language)
                .status(STATUS_PENDING)
                .triggerType(triggerType)
                .build();
        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.insert(summary);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summaryId", summary.getId());
        payload.put("language", language);
        payload.put("operatorId", operatorId);
        MeetingAiTask task = taskService.createIfAbsent(meetingId, TASK_TYPE_SUMMARY, JsonUtils.toJson(payload));

        submitAfterCommit(() -> generate(task.getId(), summary.getId(), meetingId, meeting.getTopic(), language));
        log.info("触发会议总结 - 会议ID: {}, 总结ID: {}, 触发方式: {}", meetingId, summary.getId(), triggerType);
        return toVO(summary);
    }

    @Override
    public MeetingSummaryVO regenerate(Long summaryId, Long operatorId) {
        MeetingSummary previous = summaryMapper.selectById(summaryId);
        if (previous == null || Boolean.TRUE.equals(previous.getDeleted())) {
            throw new NotFoundException(404, "总结记录不存在");
        }
        return trigger(previous.getMeetingId(), operatorId, "manual");
    }

    @Override
    public void triggerAutoIfEnabled(Long meetingId) {
        try {
            if (!properties.isEnabled()) {
                return;
            }
            MeetingAiSettings settings = settingsService.getOrDefault(meetingId);
            if (!Boolean.TRUE.equals(settings.getSummaryEnabled())
                    || !Boolean.TRUE.equals(settings.getAutoSummary())) {
                return;
            }
            trigger(meetingId, null, "auto");
        } catch (Exception e) {
            log.error("自动生成会议总结失败 - 会议ID: {}", meetingId, e);
        }
    }

    /**
     * 实际生成逻辑，运行在独立线程中
     */
    private void generate(Long taskId, Long summaryId, Long meetingId, String topic, String language) {
        taskService.markRunning(taskId);
        MeetingSummary summary = summaryMapper.selectById(summaryId);
        if (summary == null) {
            log.warn("总结记录不存在，跳过生成 - 总结ID: {}", summaryId);
            taskService.markFailed(taskId, "总结记录不存在");
            return;
        }

        summary.setStatus(STATUS_GENERATING);
        summary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.updateById(summary);

        try {
            List<MeetingTranscript> transcripts = transcriptService.getFinalTranscriptsByMeetingId(meetingId);
            if (transcripts.isEmpty()) {
                markSummaryFailed(summary, "会议没有可用的有效内容（未开启语音识别或无人发言）");
                taskService.markFailed(taskId, "没有可用的会议逐字稿");
                broadcastSummary(summary);
                return;
            }

            String dialog = renderDialog(transcripts);
            int[] tokenUsage = new int[2];
            String rawJson;
            if (dialog.length() > properties.getSummaryReduceThreshold()) {
                rawJson = summarizeByChunks(meetingId, topic, language, dialog, tokenUsage);
            } else {
                DashScopeClient.ChatResult result = dashScopeClient.chat(properties.getSummaryModel(),
                        SUMMARY_SYSTEM_PROMPT, buildSummaryPrompt(topic, language, dialog));
                rawJson = result.getContent();
                tokenUsage[0] = result.getPromptTokens();
                tokenUsage[1] = result.getCompletionTokens();
            }

            fillSummary(summary, parseSummaryJson(rawJson), language, tokenUsage);
            summaryMapper.updateById(summary);
            taskService.markSuccess(taskId, summary.getId());
            broadcastSummary(summary);
            log.info("会议总结生成完成 - 会议ID: {}, 总结ID: {}, 输入字数: {}",
                    meetingId, summary.getId(), dialog.length());
        } catch (Exception e) {
            log.error("生成会议总结失败 - 会议ID: {}, 总结ID: {}", meetingId, summaryId, e);
            markSummaryFailed(summary, e.getMessage());
            taskService.markFailed(taskId, e.getMessage());
            broadcastSummary(summary);
        }
    }

    /**
     * 长会议分段提炼后再汇总
     */
    private String summarizeByChunks(Long meetingId, String topic, String language,
                                     String dialog, int[] tokenUsage) {
        List<String> chunks = splitByChars(dialog, properties.getSummaryChunkChars());
        List<String> partials = new ArrayList<>();
        for (String chunk : chunks) {
            DashScopeClient.ChatResult result = dashScopeClient.chat(properties.getSummaryModel(),
                    CHUNK_SYSTEM_PROMPT,
                    "输出语言：" + languageName(language) + "\n会议逐字稿片段：\n" + chunk);
            partials.add(result.getContent());
            tokenUsage[0] += result.getPromptTokens();
            tokenUsage[1] += result.getCompletionTokens();
        }
        log.info("长会议分段提炼完成 - 会议ID: {}, 分段数: {}", meetingId, chunks.size());

        String merged = "以下是分段提炼的会议要点，请合并去重后生成最终纪要：\n"
                + String.join("\n---\n", partials);
        DashScopeClient.ChatResult result = dashScopeClient.chat(properties.getSummaryModel(),
                SUMMARY_SYSTEM_PROMPT, buildSummaryPrompt(topic, language, merged));
        tokenUsage[0] += result.getPromptTokens();
        tokenUsage[1] += result.getCompletionTokens();
        return result.getContent();
    }

    private String buildSummaryPrompt(String topic, String language, String content) {
        return "会议主题：" + (topic != null ? topic : "未命名会议") + "\n"
                + "输出语言：" + languageName(language) + "\n"
                + "会议内容：\n" + content + "\n\n"
                + "请严格输出以下结构的 JSON（不要输出代码块标记）：\n"
                + "{\"title\":\"会议标题\",\"overview\":\"会议概述（150字以内）\","
                + "\"keyPoints\":[\"关键要点\"],\"decisions\":[\"达成的决议\"],"
                + "\"actionItems\":[{\"owner\":\"负责人\",\"task\":\"待办内容\",\"dueDate\":\"截止时间\"}],"
                + "\"keywords\":[\"关键词\"]}\n"
                + "要求：没有的内容用空数组表示；不要编造逐字稿中不存在的信息；所有文本使用指定输出语言。";
    }

    private String renderDialog(List<MeetingTranscript> transcripts) {
        return transcripts.stream()
                .map(t -> "[" + formatSeconds(t.getStartTime()) + "] "
                        + (t.getUserName() != null ? t.getUserName() : "参会人") + "：" + t.getTextContent())
                .collect(Collectors.joining("\n"));
    }

    private String formatSeconds(Integer seconds) {
        int total = seconds != null ? Math.max(seconds, 0) : 0;
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private List<String> splitByChars(String text, int chunkChars) {
        int size = chunkChars > 0 ? chunkChars : 3000;
        List<String> chunks = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + size, text.length());
            // 尽量按行断开，避免把一句话拆散
            int newline = text.lastIndexOf('\n', end);
            if (newline > index + size / 2) {
                end = newline;
            }
            chunks.add(text.substring(index, end));
            index = end;
        }
        return chunks;
    }

    private JsonNode parseSummaryJson(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(500, "AI 返回的纪要格式无法解析");
        }
    }

    private String extractJson(String raw) {
        String text = raw != null ? raw.trim() : "";
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            text = firstLineEnd >= 0 ? text.substring(firstLineEnd + 1) : text;
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void fillSummary(MeetingSummary summary, JsonNode root, String language, int[] tokenUsage) {
        summary.setLanguage(language);
        summary.setTitle(truncate(root.path("title").asText("会议纪要"), 200));
        summary.setOverview(root.path("overview").asText(""));
        summary.setKeyPoints(toJson(root.path("keyPoints")));
        summary.setDecisions(toJson(root.path("decisions")));
        summary.setActionItems(toJson(root.path("actionItems")));
        summary.setKeywords(toJson(root.path("keywords")));
        summary.setModel(properties.getSummaryModel());
        summary.setPromptTokens(tokenUsage[0]);
        summary.setCompletionTokens(tokenUsage[1]);
        summary.setStatus(STATUS_SUCCESS);
        summary.setErrorMessage(null);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());
    }

    private void markSummaryFailed(MeetingSummary summary, String errorMessage) {
        summary.setStatus(STATUS_FAILED);
        summary.setErrorMessage(truncate(errorMessage, 500));
        summary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.updateById(summary);
    }

    /**
     * 事务内触发时延迟到提交后再执行，避免异步线程读不到刚写入的记录
     */
    private void submitAfterCommit(Runnable task) {
        Runnable safeTask = () -> {
            try {
                taskExecutor.execute(task);
            } catch (Exception e) {
                log.warn("提交 AI 总结任务失败", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeTask.run();
                }
            });
        } else {
            safeTask.run();
        }
    }

    private void broadcastSummary(MeetingSummary summary) {
        try {
            MeetingSummaryVO vo = toVO(summary);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", vo.getId());
            payload.put("meetingId", vo.getMeetingId());
            payload.put("status", vo.getStatus());
            payload.put("title", vo.getTitle());
            payload.put("overview", vo.getOverview());
            payload.put("keyPoints", vo.getKeyPoints());
            payload.put("decisions", vo.getDecisions());
            payload.put("actionItems", vo.getActionItems());
            payload.put("keywords", vo.getKeywords());
            payload.put("errorMessage", vo.getErrorMessage());
            payload.put("generatedAt", vo.getGeneratedAt());

            MessageSendDto message = new MessageSendDto();
            message.setMeetingId(summary.getMeetingId());
            message.setSenderName("AI 助手");
            message.setMessageType(WsMessageType.SUMMARY.name());
            message.setContent(JsonUtils.toJson(payload));
            message.setTargetScope(MessageSend2TypeEnum.GROUP.getCode());
            message.setSentAt(LocalDateTime.now());
            channelContextUtils.sendMessage(message);
        } catch (Exception e) {
            // 推送失败不影响已生成的纪要结果
            log.warn("推送AI纪要失败 - 会议ID: {}", summary.getMeetingId(), e);
        }
    }

    private MeetingSummary findLatestInProgress(Long meetingId) {
        return summaryMapper.selectOne(new LambdaQueryWrapper<MeetingSummary>()
                .eq(MeetingSummary::getMeetingId, meetingId)
                .in(MeetingSummary::getStatus, STATUS_PENDING, STATUS_GENERATING)
                .orderByDesc(MeetingSummary::getId)
                .last("LIMIT 1"));
    }

    private Meeting requireMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || Boolean.TRUE.equals(meeting.getDeleted())) {
            throw new NotFoundException(404, "会议不存在");
        }
        return meeting;
    }

    private String toJson(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.convertValue(arrayNode, List.class));
        } catch (Exception e) {
            return "[]";
        }
    }

    private MeetingSummaryVO toVO(MeetingSummary summary) {
        MeetingSummaryVO vo = new MeetingSummaryVO();
        vo.setId(summary.getId());
        vo.setMeetingId(summary.getMeetingId());
        vo.setSummaryType(summary.getSummaryType());
        vo.setLanguage(summary.getLanguage());
        vo.setTitle(summary.getTitle());
        vo.setOverview(summary.getOverview());
        vo.setKeyPoints(readStringList(summary.getKeyPoints()));
        vo.setDecisions(readStringList(summary.getDecisions()));
        vo.setActionItems(readActionItems(summary.getActionItems()));
        vo.setKeywords(readStringList(summary.getKeywords()));
        vo.setModel(summary.getModel());
        vo.setStatus(summary.getStatus());
        vo.setTriggerType(summary.getTriggerType());
        vo.setErrorMessage(summary.getErrorMessage());
        vo.setGeneratedAt(summary.getGeneratedAt());
        vo.setCreatedAt(summary.getCreatedAt());
        return vo;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析总结 JSON 数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private List<MeetingSummaryVO.ActionItemVO> readActionItems(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MeetingSummaryVO.ActionItemVO>>() {
            });
        } catch (Exception e) {
            log.warn("解析总结待办事项失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String languageName(String code) {
        if (code == null) {
            return "中文";
        }
        switch (code.toLowerCase()) {
            case "zh":
                return "中文";
            case "en":
                return "英文";
            case "ja":
                return "日文";
            case "ko":
                return "韩文";
            case "fr":
                return "法文";
            case "de":
                return "德文";
            case "es":
                return "西班牙文";
            case "ru":
                return "俄文";
            case "pt":
                return "葡萄牙文";
            default:
                return code;
        }
    }
}
