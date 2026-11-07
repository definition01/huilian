package cn.cxdproject.coder.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 会议 AI 配置更新入参
 *
 * @author heathcetide
 */
@Data
public class MeetingAiSettingsDTO {

    /**
     * 是否开启实时翻译
     */
    private Boolean translationEnabled;

    /**
     * 源语言，auto 表示自动识别
     */
    private String sourceLanguage;

    /**
     * 目标语言列表，如 ["en", "ja"]
     */
    private List<String> targetLanguages;

    /**
     * 是否允许生成 AI 总结
     */
    private Boolean summaryEnabled;

    /**
     * 总结输出语言
     */
    private String summaryLanguage;

    /**
     * 会议结束后是否自动生成总结
     */
    private Boolean autoSummary;
}
