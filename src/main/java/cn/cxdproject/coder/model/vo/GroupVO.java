package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群聊 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupVO {

    /**
     * 群ID
     */
    private Long id;

    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群号
     */
    private String groupNo;

    /**
     * 群主用户ID
     */
    private Long ownerId;

    /**
     * 群主用户名
     */
    private String ownerName;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 当前用户在群内的角色: owner/admin/member
     */
    private String myRole;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 群状态: normal-正常, banned-已封禁
     */
    private String status;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后一条消息时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 是否置顶
     */
    private Boolean pinned;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
