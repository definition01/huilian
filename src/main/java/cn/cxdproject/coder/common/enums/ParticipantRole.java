package cn.cxdproject.coder.common.enums;

/**
 * 会议参与者角色枚举
 *
 * @author heathcetide
 */
public enum ParticipantRole {

    /**
     * 主持人 - 会议创建者，拥有所有权限
     */
    HOST("host", "主持人"),

    /**
     * 联席主持人 - 协助主持人管理会议
     */
    CO_HOST("co_host", "联席主持人"),

    /**
     * 参会者 - 普通参会人员
     */
    PARTICIPANT("participant", "参会者");

    private final String code;
    private final String description;

    ParticipantRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantRole fromCode(String code) {
        for (ParticipantRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown participant role code: " + code);
    }
}
