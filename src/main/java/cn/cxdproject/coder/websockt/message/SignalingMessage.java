package cn.cxdproject.coder.websockt.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC 信令消息
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignalingMessage {

    /**
     * 信令类型
     */
    private SignalingType signalingType;

    /**
     * 会议ID
     */
    private String meetingId;

    /**
     * 发送者用户ID
     */
    private String fromUserId;

    /**
     * 接收者用户ID（一对一通话时使用）
     */
    private String toUserId;

    /**
     * SDP 信息（OFFER/ANSWER 时使用）
     */
    private SdpInfo sdp;

    /**
     * ICE Candidate 信息
     */
    private IceCandidateInfo iceCandidate;

    /**
     * 媒体状态（音频/视频是否开启）
     */
    private MediaState mediaState;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * SDP 信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SdpInfo {
        /**
         * SDP 类型：offer 或 answer
         */
        private String type;

        /**
         * SDP 描述
         */
        private String sdp;
    }

    /**
     * ICE Candidate 信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IceCandidateInfo {
        /**
         * Candidate 字符串
         */
        private String candidate;

        /**
         * sdpMid
         */
        private String sdpMid;

        /**
         * sdpMLineIndex
         */
        private Integer sdpMLineIndex;
    }

    /**
     * 媒体状态
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MediaState {
        /**
         * 音频是否开启
         */
        private Boolean audioEnabled;

        /**
         * 视频是否开启
         */
        private Boolean videoEnabled;

        /**
         * 是否正在屏幕共享
         */
        private Boolean screenSharing;
    }
}
