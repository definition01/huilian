package cn.cxdproject.coder.service;

import cn.cxdproject.coder.config.VolcengineAsrConfig;
import cn.cxdproject.coder.model.MessageSend2TypeEnum;
import cn.cxdproject.coder.model.dto.MessageSendDto;
import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.service.ai.RealtimeTranslationService;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import cn.cxdproject.coder.websockt.message.WsMessageType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * 火山引擎 ASR 服务（流式语音识别大模型 v3，二进制帧协议）
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class VolcengineAsrService {

    @Resource
    private VolcengineAsrConfig asrConfig;

    @Resource
    private MeetingTranscriptService transcriptService;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource
    private RealtimeTranslationService realtimeTranslationService;

    // 存储每个会议的ASR客户端
    private final Map<String, AsrWebSocketClient> meetingAsrClients = new ConcurrentHashMap<>();

    /**
     * 为会议创建ASR连接
     *
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param resultCallback 识别结果回调
     */
    public void startAsr(Long meetingId, Long userId, String userName, Consumer<String> resultCallback) {
        if (!asrConfig.getEnabled()) {
            log.warn("ASR功能未启用");
            return;
        }

        String key = meetingId + "_" + userId;

        if (meetingAsrClients.containsKey(key)) {
            log.warn("用户{}的ASR连接已存在", userName);
            return;
        }

        try {
            AsrWebSocketClient client = new AsrWebSocketClient(
                    new URI(asrConfig.getEndpoint()),
                    meetingId,
                    userId,
                    userName,
                    resultCallback
            );

            client.connect();
            meetingAsrClients.put(key, client);
            log.info("为用户{}创建ASR连接 - 会议ID: {}", userName, meetingId);

        } catch (Exception e) {
            log.error("创建ASR连接失败", e);
        }
    }

    /**
     * 发送音频数据到ASR
     *
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param audioData 音频数据（PCM格式）
     */
    public void sendAudio(Long meetingId, Long userId, byte[] audioData) {
        String key = meetingId + "_" + userId;
        AsrWebSocketClient client = meetingAsrClients.get(key);

        if (client != null && client.isOpen()) {
            client.sendAudioFrame(audioData);
        } else {
            // 音频按固定频率上报，这里降为 trace，避免刷日志
            log.trace("ASR连接不存在或已关闭 - 会议ID: {}, 用户ID: {}", meetingId, userId);
        }
    }

    /**
     * 停止ASR连接
     *
     * @param meetingId 会议ID
     * @param userId 用户ID
     */
    public void stopAsr(Long meetingId, Long userId) {
        String key = meetingId + "_" + userId;
        AsrWebSocketClient client = meetingAsrClients.remove(key);

        if (client != null) {
            // 发送结束帧后由服务端返回最终结果并主动关闭连接
            client.sendLastFrame();
            log.info("关闭ASR连接 - 会议ID: {}, 用户ID: {}", meetingId, userId);
        }
    }

    /**
     * ASR WebSocket 客户端
     */
    private class AsrWebSocketClient extends WebSocketClient {

        private final Long meetingId;
        private final Long userId;
        private final String userName;
        private final Consumer<String> resultCallback;
        private long savedUtteranceEnd = 0;
        private String lastSubtitle = "";
        private boolean lastFrameSent = false;
        /**
         * 配置帧发送完成前不允许发送音频帧，避免服务端报
         * "no request object before data" 后直接断连
         */
        private volatile boolean configSent = false;

        public AsrWebSocketClient(URI serverUri, Long meetingId, Long userId,
                                 String userName, Consumer<String> resultCallback) {
            super(serverUri);
            this.meetingId = meetingId;
            this.userId = userId;
            this.userName = userName;
            this.resultCallback = resultCallback;

            // 大模型流式识别使用请求头鉴权
            addHeader("X-Api-App-Key", asrConfig.getAppId());
            addHeader("X-Api-Key", asrConfig.getAccessKey());
            addHeader("X-Api-Resource-Id", asrConfig.getResourceId());
            addHeader("X-Api-Connect-Id", UUID.randomUUID().toString());
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            log.info("ASR WebSocket连接已建立 - 用户: {}", userName);
            sendConfigFrame();
        }

        /**
         * 发送识别配置（full client request）
         */
        private void sendConfigFrame() {
            JSONObject user = new JSONObject();
            user.put("uid", String.valueOf(userId));

            JSONObject audio = new JSONObject();
            audio.put("format", asrConfig.getFormat());
            audio.put("codec", "raw");
            audio.put("rate", asrConfig.getSampleRate());
            audio.put("bits", 16);
            audio.put("channel", 1);

            JSONObject request = new JSONObject();
            request.put("model_name", "bigmodel");
            request.put("enable_itn", true);
            request.put("enable_punc", true);
            request.put("show_utterances", true);
            request.put("result_type", "single");

            JSONObject config = new JSONObject();
            config.put("user", user);
            config.put("audio", audio);
            config.put("request", request);

            send(buildFrame(0x01, 0x00, 0x01, 0x00,
                    config.toJSONString().getBytes(StandardCharsets.UTF_8)));
            configSent = true;
        }

        /**
         * 发送音频帧（audio only request）
         */
        void sendAudioFrame(byte[] audioData) {
            if (!configSent || !isOpen() || lastFrameSent || audioData == null || audioData.length == 0) {
                return;
            }
            send(buildFrame(0x02, 0x00, 0x00, 0x00, audioData));
        }

        /**
         * 发送最后一帧，通知服务端音频结束
         */
        void sendLastFrame() {
            if (!configSent || !isOpen() || lastFrameSent) {
                return;
            }
            lastFrameSent = true;
            send(buildFrame(0x02, 0x02, 0x00, 0x00, new byte[0]));
        }

        /**
         * 构造火山二进制帧：4 字节头 + 4 字节 payload 长度 + payload
         */
        private byte[] buildFrame(int messageType, int flags, int serialization, int compression, byte[] payload) {
            byte[] frame = new byte[8 + payload.length];
            // version=1, header size=1（4 字节）
            frame[0] = 0x11;
            frame[1] = (byte) ((messageType << 4) | flags);
            frame[2] = (byte) ((serialization << 4) | compression);
            frame[3] = 0x00;
            frame[4] = (byte) ((payload.length >> 24) & 0xFF);
            frame[5] = (byte) ((payload.length >> 16) & 0xFF);
            frame[6] = (byte) ((payload.length >> 8) & 0xFF);
            frame[7] = (byte) (payload.length & 0xFF);
            System.arraycopy(payload, 0, frame, 8, payload.length);
            return frame;
        }

        @Override
        public void onMessage(String message) {
            // 兼容服务端以文本帧下发的情况，截取 JSON 部分
            int jsonStart = message.indexOf('{');
            if (jsonStart >= 0) {
                handleJson(message.substring(jsonStart));
            }
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            handleBinaryFrame(bytes);
        }

        /**
         * 解析二进制响应帧
         */
        private void handleBinaryFrame(ByteBuffer buffer) {
            try {
                byte[] raw = new byte[buffer.remaining()];
                buffer.get(raw);
                if (raw.length < 8) {
                    return;
                }

                int headerSize = (raw[0] & 0x0F) * 4;
                int messageType = (raw[1] >> 4) & 0x0F;
                int flags = raw[1] & 0x0F;
                int compression = raw[2] & 0x0F;

                int offset = headerSize;
                // 带序列号的响应帧在长度字段前多 4 字节序列号
                if ((flags & 0x01) != 0) {
                    offset += 4;
                }
                // 错误帧在长度字段前多 4 字节错误码
                if (messageType == 0x0F) {
                    offset += 4;
                }
                if (raw.length < offset + 4) {
                    return;
                }

                int payloadLength = ((raw[offset] & 0xFF) << 24)
                        | ((raw[offset + 1] & 0xFF) << 16)
                        | ((raw[offset + 2] & 0xFF) << 8)
                        | (raw[offset + 3] & 0xFF);
                int payloadStart = offset + 4;
                if (payloadLength <= 0 || payloadStart + payloadLength > raw.length) {
                    return;
                }

                byte[] payload = new byte[payloadLength];
                System.arraycopy(raw, payloadStart, payload, 0, payloadLength);
                if (compression == 0x01) {
                    payload = gunzip(payload);
                }

                String json = new String(payload, StandardCharsets.UTF_8);
                if (messageType == 0x0F) {
                    log.error("ASR识别返回错误 - 用户: {}, 响应: {}", userName, json);
                    return;
                }
                handleJson(json);
            } catch (Exception e) {
                log.error("解析ASR响应帧失败 - 用户: {}", userName, e);
            }
        }

        private byte[] gunzip(byte[] data) throws Exception {
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return out.toByteArray();
            }
        }

        /**
         * 处理识别结果
         */
        private void handleJson(String payload) {
            try {
                JSONObject json = JSON.parseObject(payload);
                int code = json.getIntValue("code");
                if (code != 0 && code != 20000000) {
                    log.error("ASR识别返回错误 - 用户: {}, code: {}, message: {}",
                            userName, code, json.getString("message"));
                    return;
                }

                JSONObject result = json.getJSONObject("result");
                if (result == null) {
                    return;
                }

                String fullText = result.getString("text");
                JSONArray utterances = result.getJSONArray("utterances");
                String subtitle = null;

                if (utterances != null) {
                    for (int i = 0; i < utterances.size(); i++) {
                        JSONObject utterance = utterances.getJSONObject(i);
                        String text = utterance.getString("text");
                        if (text == null || text.isEmpty()) {
                            continue;
                        }
                        subtitle = text;

                        long endTime = utterance.getLongValue("end_time");
                        if (utterance.getBooleanValue("definite") && endTime > savedUtteranceEnd) {
                            saveUtterance(text, utterance.getLongValue("start_time"), endTime);
                            savedUtteranceEnd = endTime;
                        }
                    }
                }

                if (subtitle == null) {
                    subtitle = fullText;
                }

                // 中间结果实时推送字幕，文本不变时不重复推送
                if (subtitle != null && !subtitle.isEmpty() && !subtitle.equals(lastSubtitle)) {
                    pushSubtitle(subtitle);
                    lastSubtitle = subtitle;
                }
            } catch (Exception e) {
                log.error("处理ASR结果失败", e);
            }
        }

        /**
         * 保存已定稿的语句并触发翻译
         */
        private void saveUtterance(String text, long startTime, long endTime) {
            int startSeconds = startTime > 0 ? (int) (startTime / 1000) : (int) (savedUtteranceEnd / 1000);
            int endSeconds = endTime > 0 ? (int) (endTime / 1000) : startSeconds + 3;

            MeetingTranscript transcript = MeetingTranscript.builder()
                    .meetingId(meetingId)
                    .userId(userId)
                    .userName(userName)
                    .textContent(text)
                    .startTime(startSeconds)
                    .endTime(endSeconds)
                    .confidence(1.0)
                    .isFinal(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transcriptService.saveTranscript(transcript);

            // 最终结果触发实时翻译（异步，不阻塞ASR线程）
            realtimeTranslationService.translateFinalTranscript(transcript);

            if (resultCallback != null) {
                resultCallback.accept(text);
            }
        }

        /**
         * 推送字幕到所有会议参与者
         */
        private void pushSubtitle(String text) {
            MessageSendDto subtitleMsg = new MessageSendDto();
            subtitleMsg.setMessageType(WsMessageType.SUBTITLE.name());
            subtitleMsg.setMeetingId(meetingId);
            subtitleMsg.setTargetScope(MessageSend2TypeEnum.GROUP.getCode());
            subtitleMsg.setSenderId(userId);
            subtitleMsg.setSenderName(userName);
            subtitleMsg.setContent(text);
            subtitleMsg.setSentAt(LocalDateTime.now());

            channelContextUtils.sendMessage(subtitleMsg);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            meetingAsrClients.remove(meetingId + "_" + userId);
            log.info("ASR WebSocket连接已关闭 - 用户: {}, code: {}, 原因: {}", userName, code, reason);
        }

        @Override
        public void onError(Exception ex) {
            log.error("ASR WebSocket错误 - 用户: {}", userName, ex);
        }
    }
}
