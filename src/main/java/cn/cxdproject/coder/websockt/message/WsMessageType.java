package cn.cxdproject.coder.websockt.message;

/**
 * WebSocket 消息类型
 *
 * @author heathcetide
 */
public enum WsMessageType {

    /**
     * 文本消息（聊天）
     */
    TEXT,

    /**
     * 控制消息（静音、举手、屏幕共享等）
     */
    CONTROL,

    /**
     * 系统消息（用户加入、离开等）
     */
    SYSTEM,

    /**
     * 心跳消息
     */
    HEARTBEAT,

    /**
     * 音视频信令（SDP、ICE等）
     */
    SIGNALING,

    /**
     * ASR字幕消息
     */
    SUBTITLE,

    /**
     * 音频数据消息（用于ASR）
     */
    AUDIO,

    /**
     * 实时翻译消息
     */
    TRANSLATION,

    /**
     * AI 会议总结消息
     */
    SUMMARY,

    /**
     * 群聊消息
     */
    GROUP_MESSAGE,

    /**
     * 私聊消息
     */
    PRIVATE_MESSAGE,

    /**
     * 错误消息
     */
    ERROR
}
