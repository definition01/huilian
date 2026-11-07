package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.mapper.MeetingAiSettingsMapper;
import cn.cxdproject.coder.exception.BusinessException;
import cn.cxdproject.coder.model.dto.MeetingAiSettingsDTO;
import cn.cxdproject.coder.model.entity.MeetingAiSettings;
import cn.cxdproject.coder.model.vo.MeetingAiSettingsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 会议 AI 配置服务实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingAiSettingsServiceImpl extends ServiceImpl<MeetingAiSettingsMapper, MeetingAiSettings>
        implements MeetingAiSettingsService {

    private static final String DEFAULT_TARGET_LANGUAGES = "[\"en\"]";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MeetingAiSettings getOrDefault(Long meetingId) {
        MeetingAiSettings settings = getByMeetingId(meetingId);
        if (settings != null) {
            return settings;
        }
        return MeetingAiSettings.builder()
                .meetingId(meetingId)
                .translationEnabled(false)
                .sourceLanguage("auto")
                .targetLanguages(DEFAULT_TARGET_LANGUAGES)
                .summaryEnabled(true)
                .summaryLanguage("zh")
                .autoSummary(true)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingAiSettings saveSettings(Long meetingId, MeetingAiSettingsDTO dto) {
        MeetingAiSettings settings = getByMeetingId(meetingId);
        boolean isNew = settings == null;
        if (isNew) {
            settings = MeetingAiSettings.builder()
                    .meetingId(meetingId)
                    .translationEnabled(false)
                    .sourceLanguage("auto")
                    .targetLanguages(DEFAULT_TARGET_LANGUAGES)
                    .summaryEnabled(true)
                    .summaryLanguage("zh")
                    .autoSummary(true)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        if (dto.getTranslationEnabled() != null) {
            settings.setTranslationEnabled(dto.getTranslationEnabled());
        }
        if (dto.getSourceLanguage() != null) {
            settings.setSourceLanguage(dto.getSourceLanguage());
        }
        if (dto.getTargetLanguages() != null) {
            if (dto.getTargetLanguages().isEmpty()
                    && Boolean.TRUE.equals(settings.getTranslationEnabled())) {
                throw new BusinessException(400, "开启翻译时至少选择一个目标语言");
            }
            settings.setTargetLanguages(toJsonArray(dto.getTargetLanguages()));
        }
        if (dto.getSummaryEnabled() != null) {
            settings.setSummaryEnabled(dto.getSummaryEnabled());
        }
        if (dto.getSummaryLanguage() != null) {
            settings.setSummaryLanguage(dto.getSummaryLanguage());
        }
        if (dto.getAutoSummary() != null) {
            settings.setAutoSummary(dto.getAutoSummary());
        }
        settings.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            save(settings);
        } else {
            updateById(settings);
        }
        log.info("保存会议AI配置 - 会议ID: {}, 翻译: {}, 语种: {}, 总结: {}",
                meetingId, settings.getTranslationEnabled(), settings.getTargetLanguages(), settings.getSummaryEnabled());
        return settings;
    }

    private MeetingAiSettings getByMeetingId(Long meetingId) {
        return getOne(new LambdaQueryWrapper<MeetingAiSettings>()
                .eq(MeetingAiSettings::getMeetingId, meetingId)
                .last("LIMIT 1"));
    }

    /**
     * 解析目标语言 JSON 数组
     */
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

    private String toJsonArray(List<String> languages) {
        try {
            return objectMapper.writeValueAsString(languages);
        } catch (Exception e) {
            return DEFAULT_TARGET_LANGUAGES;
        }
    }

    @Override
    public MeetingAiSettingsVO toVO(MeetingAiSettings settings) {
        MeetingAiSettingsVO vo = new MeetingAiSettingsVO();
        if (settings == null) {
            return vo;
        }
        vo.setMeetingId(settings.getMeetingId());
        vo.setTranslationEnabled(settings.getTranslationEnabled());
        vo.setSourceLanguage(settings.getSourceLanguage());
        vo.setTargetLanguages(parseTargetLanguages(settings.getTargetLanguages()));
        vo.setSummaryEnabled(settings.getSummaryEnabled());
        vo.setSummaryLanguage(settings.getSummaryLanguage());
        vo.setAutoSummary(settings.getAutoSummary());
        return vo;
    }
}
