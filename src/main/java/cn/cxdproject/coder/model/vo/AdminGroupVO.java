package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端群聊 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminGroupVO {

    private Long id;

    private String groupName;

    private String groupNo;

    private Long ownerId;

    /**
     * 群主用户名
     */
    private String ownerName;

    /**
     * 成员数量
     */
    private Integer memberCount;

    private String announcement;

    /**
     * 群状态: normal-正常, banned-已封禁
     */
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
