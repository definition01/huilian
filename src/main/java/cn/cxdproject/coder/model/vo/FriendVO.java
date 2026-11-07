package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendVO {

    /**
     * 好友用户ID
     */
    private Long userId;

    /**
     * 好友用户名
     */
    private String username;

    /**
     * 好友头像
     */
    private String avatar;

    /**
     * 好友手机号（脱敏）
     */
    private String mobile;

    /**
     * 我为该好友设置的备注
     */
    private String remark;

    /**
     * 最后一条私聊消息
     */
    private String lastMessage;

    /**
     * 最后一条私聊消息时间
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
}
