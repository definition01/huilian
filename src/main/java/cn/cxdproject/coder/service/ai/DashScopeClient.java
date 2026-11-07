package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.config.MeetingAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 通义千问（DashScope OpenAI 兼容）调用客户端
 * 负责请求拼装、超时控制、失败重试与 token 统计
 *
 * @author heathcetide
 */
@Slf4j
@Component
public class DashScopeClient {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final int MAX_ATTEMPTS = 3;

    @Resource
    private MeetingAiProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(properties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(properties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 单轮对话调用
     *
     * @param model        模型名称
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型输出与 token 用量
     */
    public ChatResult chat(String model, String systemPrompt, String userPrompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI 功能未启用");
        }
        if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
            throw new AiInvokeException("未配置 DASHSCOPE_API_KEY");
        }

        String requestBody;
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.2);
            ArrayNode messages = root.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", userPrompt);
            requestBody = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new AiInvokeException("构造 AI 请求失败: " + e.getMessage(), e);
        }

        long start = System.currentTimeMillis();
        Exception lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ChatResult result = doRequest(model, requestBody);
                log.info("AI 调用成功 - 模型: {}, 耗时: {}ms, tokens: {}/{}", model,
                        System.currentTimeMillis() - start, result.getPromptTokens(), result.getCompletionTokens());
                return result;
            } catch (Exception e) {
                lastError = e;
                log.warn("AI 调用失败（第 {} 次）- 模型: {}, 原因: {}", attempt, model, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new AiInvokeException("AI 调用失败: " + (lastError != null ? lastError.getMessage() : "未知错误"), lastError);
    }

    private ChatResult doRequest(String model, String requestBody) throws Exception {
        Request request = new Request.Builder()
                .url(properties.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new AiInvokeException("HTTP " + response.code() + " - " + body);
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new AiInvokeException("AI 返回内容为空: " + body);
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.trim().isEmpty()) {
                throw new AiInvokeException("AI 返回内容为空: " + body);
            }
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            return new ChatResult(content.trim(), promptTokens, completionTokens);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ChatResult {
        private String content;
        private int promptTokens;
        private int completionTokens;
    }

    public static class AiInvokeException extends RuntimeException {
        public AiInvokeException(String message) {
            super(message);
        }

        public AiInvokeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
