package cn.cxdproject.coder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 火山引擎 ASR 配置
 *
 * @author heathcetide
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "volcengine.asr")
public class VolcengineAsrConfig {

    /**
     * 火山引擎 App ID
     */
    private String appId;

    /**
     * 火山引擎 Access Key
     */
    private String accessKey;

    /**
     * 火山引擎 Secret Key
     */
    private String secretKey;

    /**
     * ASR 服务地址（流式语音识别大模型 v3）
     */
    private String endpoint = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel";

    /**
     * 资源 ID，按时长计费为 volc.bigasr.sauc.duration
     */
    private String resourceId = "volc.bigasr.sauc.duration";

    /**
     * 识别语言
     */
    private String language = "zh-CN";

    /**
     * 音频格式
     */
    private String format = "pcm";

    /**
     * 采样率
     */
    private Integer sampleRate = 16000;

    /**
     * 是否启用ASR功能
     */
    private Boolean enabled = false;
}
