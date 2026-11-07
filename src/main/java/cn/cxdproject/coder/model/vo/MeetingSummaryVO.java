package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会议总结展示对象
 *
 * @author heathcetide
 */
@Data
public class MeetingSummaryVO {

    private Long id;

    private Long meetingId;

    private String summaryType;

    private String language;

    private String title;

    private String overview;

    private List<String> keyPoints;

    private List<String> decisions;

    private List<ActionItemVO> actionItems;

    private List<String> keywords;

    private String model;

    /**
     * pending / generating / success / failed
     */
    private String status;

    private String triggerType;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 待办事项
     */
    @Data
    public static class ActionItemVO {
        /**
         * 负责人
         */
        private String owner;

        /**
         * 待办内容
         */
        private String task;

        /**
         * 截止时间或时间要求
         */
        private String dueDate;
    }
}
