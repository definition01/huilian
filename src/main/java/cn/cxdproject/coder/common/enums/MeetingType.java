package cn.cxdproject.coder.common.enums;

/**
 * 会议类型枚举
 *
 * @author heathcetide
 */
public enum MeetingType {

    /**
     * 即时会议 - 立即开始
     */
    INSTANT("instant", "即时会议"),

    /**
     * 预约会议 - 预定未来时间
     */
    SCHEDULED("scheduled", "预约会议");

    private final String code;
    private final String description;

    MeetingType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MeetingType fromCode(String code) {
        for (MeetingType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown meeting type code: " + code);
    }
}
