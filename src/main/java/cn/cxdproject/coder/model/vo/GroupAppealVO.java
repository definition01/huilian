package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群聊封禁申诉 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupAppealVO {

    private Long id;

    private Long groupId;

    private String groupName;

    private String groupNo;

    private Long ownerId;

    private String ownerName;

    private String reason;

    /**
     * 状态: PENDING/APPROVED/REJECTED
     */
    private String status;

    private String adminRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
