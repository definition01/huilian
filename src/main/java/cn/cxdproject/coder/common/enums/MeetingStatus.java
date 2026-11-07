package cn.cxdproject.coder.common.enums;

/**
 * 会议状态枚举
 *
 * @author heathcetide
 */
public enum MeetingStatus {

    /**
     * 等待中 - 预约会议尚未开始
     */
    WAITING("waiting", "等待中"),

    /**
     * 进行中 - 会议正在进行
     */
    ONGOING("ongoing", "进行中"),

    /**
     * 已结束 - 会议已正常结束
     */
    ENDED("ended", "已结束"),

    /**
     * 已取消 - 会议被取消
     */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    MeetingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MeetingStatus fromCode(String code) {
        for (MeetingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown meeting status code: " + code);
    }
}
