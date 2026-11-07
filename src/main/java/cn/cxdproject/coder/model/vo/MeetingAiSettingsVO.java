package cn.cxdproject.coder.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 会议 AI 配置展示对象
 *
 * @author heathcetide
 */
@Data
public class MeetingAiSettingsVO {

    private Long meetingId;

    private Boolean translationEnabled;

    private String sourceLanguage;

    private List<String> targetLanguages;

    private Boolean summaryEnabled;

    private String summaryLanguage;

    private Boolean autoSummary;
}
