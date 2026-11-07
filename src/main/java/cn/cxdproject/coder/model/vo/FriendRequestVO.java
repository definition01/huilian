package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友申请 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestVO {

    /**
     * 申请ID
     */
    private Long id;

    /**
     * 申请人用户ID
     */
    private Long fromUserId;

    /**
     * 申请人用户名
     */
    private String username;

    /**
     * 接收人用户ID
     */
    private Long toUserId;

    /**
     * 接收人用户名
     */
    private String toUsername;

    /**
     * 申请人头像
     */
    private String avatar;

    /**
     * 申请附言
     */
    private String applyMessage;

    /**
     * 状态: PENDING/APPROVED/REJECTED
     */
    private String status;

    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
