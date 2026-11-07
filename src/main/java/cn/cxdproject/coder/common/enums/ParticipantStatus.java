package cn.cxdproject.coder.common.enums;

/**
 * 参与者状态枚举
 *
 * @author heathcetide
 */
public enum ParticipantStatus {

    /**
     * 已邀请 - 已被邀请但未加入
     */
    INVITED("invited", "已邀请"),

    /**
     * 等候中 - 在等候室等待主持人允许
     */
    WAITING("waiting", "等候中"),

    /**
     * 已加入 - 已加入会议
     */
    JOINED("joined", "已加入"),

    /**
     * 已离开 - 已离开会议
     */
    LEFT("left", "已离开");

    private final String code;
    private final String description;

    ParticipantStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantStatus fromCode(String code) {
        for (ParticipantStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown participant status code: " + code);
    }
}
