package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.config.MeetingAiProperties;
import cn.cxdproject.coder.mapper.MeetingTranscriptMapper;
import cn.cxdproject.coder.mapper.MeetingTranslationMapper;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.MeetingAiSettings;
import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.model.entity.MeetingTranslation;
import cn.cxdproject.coder.utils.JsonUtils;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会议实时翻译服务实现
 * ASR 最终结果落库后触发，逐目标语种调用通义千问并推送 WS
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class RealtimeTranslationServiceImpl implements RealtimeTranslationService {

    private static final String TRANSLATE_SYSTEM_PROMPT =
            "你是专业的会议同声传译引擎。只输出译文，不要解释、不要加引号、不要重复原文。"
                    + "保持术语、人名、数字与单位准确，语句通顺，符合目标语言的表达习惯。";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MeetingAiProperties properties;

    @Resource
    private DashScopeClient dashScopeClient;

    @Resource
    private MeetingAiSettingsService settingsService;

    @Resource
    private MeetingTranslationMapper translationMapper;

    @Resource
    private MeetingTranscriptMapper transcriptMapper;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void translateFinalTranscript(MeetingTranscript transcript) {
        if (transcript == null || transcript.getMeetingId() == null
                || transcript.getTextContent() == null || transcript.getTextContent().trim().isEmpty()) {
            return;
        }
        try {
            taskExecutor.execute(() -> doTranslate(transcript));
        } catch (Exception e) {
            // 线程池拒绝时不影响 ASR 主流程
            log.warn("提交翻译任务失败 - 会议ID: {}, 转录ID: {}", transcript.getMeetingId(), transcript.getId(), e);
        }
    }

    private void doTranslate(MeetingTranscript transcript) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            MeetingAiSettings settings = settingsService.getOrDefault(transcript.getMeetingId());
            if (!Boolean.TRUE.equals(settings.getTranslationEnabled())) {
                return;
            }

            List<String> targetLanguages = parseTargetLanguages(settings.getTargetLanguages());
            if (targetLanguages.isEmpty()) {
                return;
            }

            String sourceLanguage = settings.getSourceLanguage() != null ? settings.getSourceLanguage() : "auto";
            String context = buildContext(transcript);

            for (String targetLanguage : targetLanguages) {
                if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
                    continue;
                }
                if (!"auto".equalsIgnoreCase(sourceLanguage)
                        && targetLanguage.equalsIgnoreCase(sourceLanguage)) {
                    continue;
                }
                translateOne(transcript, sourceLanguage, targetLanguage, context);
            }
        } catch (Exception e) {
            log.error("实时翻译失败 - 会议ID: {}, 转录ID: {}", transcript.getMeetingId(), transcript.getId(), e);
        }
    }

    private void translateOne(MeetingTranscript transcript, String sourceLanguage,
                              String targetLanguage, String context) {
        long start = System.currentTimeMillis();
        try {
            String translated = dashScopeClient.chat(properties.getTranslateModel(), TRANSLATE_SYSTEM_PROMPT,
                    buildTranslatePrompt(transcript.getTextContent(), sourceLanguage, targetLanguage, context)).getContent();
            translated = cleanTranslation(translated);
            // 目标语言与原文相同时（如同一语种的中文字幕），不再重复保存和推送
            if (isSameText(translated, transcript.getTextContent())) {
                log.debug("译文与原文一致，跳过保存 - 会议ID: {}, 语种: {}",
                        transcript.getMeetingId(), targetLanguage);
                return;
            }
            int latency = (int) (System.currentTimeMillis() - start);

            MeetingTranslation record = MeetingTranslation.builder()
                    .meetingId(transcript.getMeetingId())
                    .transcriptId(transcript.getId())
                    .userId(transcript.getUserId())
                    .userName(transcript.getUserName())
                    .segmentIndex(resolveSegmentIndex(transcript))
                    .sourceLanguage(sourceLanguage)
                    .targetLanguage(targetLanguage)
                    .sourceText(transcript.getTextContent())
                    .translatedText(translated)
                    .engine("llm")
                    .latencyMs(latency)
                    .status("success")
                    .isFinal(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            translationMapper.insert(record);

            broadcastTranslation(record);
            log.info("实时翻译完成 - 会议ID: {}, 语种: {}, 耗时: {}ms",
                    transcript.getMeetingId(), targetLanguage, latency);
        } catch (Exception e) {
            log.warn("翻译片段失败 - 会议ID: {}, 语种: {}, 原因: {}",
                    transcript.getMeetingId(), targetLanguage, e.getMessage());
        }
    }

    private void broadcastTranslation(MeetingTranslation record) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", record.getId());
        payload.put("transcriptId", record.getTranscriptId());
        payload.put("targetLanguage", record.getTargetLanguage());
        payload.put("sourceLanguage", record.getSourceLanguage());
        payload.put("sourceText", record.getSourceText());
        payload.put("translatedText", record.getTranslatedText());
        payload.put("userName", record.getUserName());
        payload.put("segmentIndex", record.getSegmentIndex());
        payload.put("sentAt", LocalDateTime.now());

        MessageSendDto message = new MessageSendDto();
        message.setMeetingId(record.getMeetingId());
        message.setSenderId(record.getUserId());
        message.setSenderName(record.getUserName());
        message.setMessageType(WsMessageType.TRANSLATION.name());
        message.setContent(JsonUtils.toJson(payload));
        message.setTargetScope(MessageSend2TypeEnum.GROUP.getCode());
        message.setSentAt(LocalDateTime.now());
        channelContextUtils.sendMessage(message);
    }

    private String buildTranslatePrompt(String text, String sourceLanguage,
                                        String targetLanguage, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("目标语言：").append(languageName(targetLanguage))
                .append("(").append(targetLanguage).append(")\n");
        prompt.append("源语言：")
                .append("auto".equalsIgnoreCase(sourceLanguage) ? "自动识别" : languageName(sourceLanguage))
                .append("\n");
        if (context != null && !context.isEmpty()) {
            prompt.append("前文上下文（仅用于理解语境，不要翻译这部分内容）：\n")
                    .append(context).append("\n");
        }
        prompt.append("需要翻译的句子：\n").append(text);
        return prompt.toString();
    }

    /**
     * 判断译文与原文是否实质相同（忽略空白和常见标点）
     */
    private boolean isSameText(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return normalizeText(a).equals(normalizeText(b));
    }

    private String normalizeText(String text) {
        return text.replaceAll("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】…—·]", "").toLowerCase();
    }

    /**
     * 去掉模型可能附带的引号、译文字样等多余内容
     */
    private String cleanTranslation(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("“") && cleaned.endsWith("”")))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    /**
     * 取当前片段之前的若干条最终转录，拼成上下文
     */
    private String buildContext(MeetingTranscript current) {
        int maxSegments = properties.getMaxContextSegments();
        if (maxSegments <= 0 || current.getId() == null) {
            return "";
        }
        List<MeetingTranscript> history = transcriptMapper.selectList(new LambdaQueryWrapper<MeetingTranscript>()
                .eq(MeetingTranscript::getMeetingId, current.getMeetingId())
                .eq(MeetingTranscript::getIsFinal, true)
                .lt(MeetingTranscript::getId, current.getId())
                .orderByDesc(MeetingTranscript::getId)
                .last("LIMIT " + maxSegments));

        Collections.reverse(history);
        return history.stream()
                .map(t -> t.getUserName() + "：" + t.getTextContent())
                .collect(Collectors.joining("\n"));
    }

    private Integer resolveSegmentIndex(MeetingTranscript transcript) {
        if (transcript.getStartTime() != null) {
            return transcript.getStartTime() / 3;
        }
        return transcript.getId() != null ? transcript.getId().intValue() : 0;
    }

    private List<String> parseTargetLanguages(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析目标语言失败: {}", json, e);
            return Collections.emptyList();
        }
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

    @Override
    public List<MeetingTranslation> listByMeeting(Long meetingId, String targetLanguage) {
        LambdaQueryWrapper<MeetingTranslation> wrapper = new LambdaQueryWrapper<MeetingTranslation>()
                .eq(MeetingTranslation::getMeetingId, meetingId)
                .eq(targetLanguage != null && !targetLanguage.isEmpty(),
                        MeetingTranslation::getTargetLanguage, targetLanguage)
                .orderByAsc(MeetingTranslation::getId);
        return translationMapper.selectList(wrapper);
    }
}
