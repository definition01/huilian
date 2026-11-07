package cn.cxdproject.coder.websockt.message;

/**
 * WebRTC 信令类型
 *
 * @author heathcetide
 */
public enum SignalingType {

    /**
     * Offer - 发起方发送的 SDP
     */
    OFFER,

    /**
     * Answer - 接收方回复的 SDP
     */
    ANSWER,

    /**
     * ICE Candidate - ICE 候选者
     */
    ICE_CANDIDATE,

    /**
     * 加入房间
     */
    JOIN_ROOM,

    /**
     * 离开房间
     */
    LEAVE_ROOM,

    /**
     * 准备好接收连接
     */
    READY,

    /**
     * 请求重新协商
     */
    RENEGOTIATE,

    /**
     * 媒体状态变化（音频/视频开关）
     */
    MEDIA_STATE_CHANGE
}
