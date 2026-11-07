package cn.cxdproject.coder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 会议 AI（实时翻译 / 智能总结）配置
 * 大模型统一走通义千问 DashScope 的 OpenAI 兼容接口
 *
 * @author heathcetide
 */
@Data
@Component
@ConfigurationProperties(prefix = "meeting.ai")
public class MeetingAiProperties {

    /**
     * AI 功能总开关
     */
    private boolean enabled = false;

    /**
     * DashScope OpenAI 兼容接口地址
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * DashScope API Key
     */
    private String apiKey = "";

    /**
     * 实时翻译模型
     * 说明：qwen-mt 系列需要额外的 translation_options 参数，
     * 走通用 chat/completions 时请使用 qwen-plus 等通用模型
     */
    private String translateModel = "qwen-plus";

    /**
     * 会议总结模型
     */
    private String summaryModel = "qwen-plus";

    /**
     * 单次请求超时（秒）
     */
    private int timeoutSeconds = 60;

    /**
     * 翻译时携带的上文片段数，用于保持上下文连贯
     */
    private int maxContextSegments = 5;

    /**
     * 总结时单块最大字符数，超过则先分段提炼再汇总
     */
    private int summaryChunkChars = 3000;

    /**
     * 总结输入超过该字符数才启用 map-reduce
     */
    private int summaryReduceThreshold = 8000;
}
