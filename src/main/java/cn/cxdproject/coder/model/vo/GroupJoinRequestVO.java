package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 入群申请 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupJoinRequestVO {

    /**
     * 申请ID
     */
    private Long id;

    /**
     * 群ID
     */
    private Long groupId;

    /**
     * 申请人用户ID
     */
    private Long userId;

    /**
     * 申请人用户名
     */
    private String username;

    /**
     * 申请人头像
     */
    private String avatar;

    /**
     * 邀请人用户ID，为空表示主动申请
     */
    private Long inviterId;

    /**
     * 邀请人用户名
     */
    private String inviterName;

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
