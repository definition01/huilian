package cn.cxdproject.coder.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群号搜索结果 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupSearchVO {

    /**
     * 群ID
     */
    private Long id;

    /**
     * 群号
     */
    private String groupNo;

    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群主用户名
     */
    private String ownerName;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 当前用户是否已在群内
     */
    private Boolean joined;

    /**
     * 当前用户是否有待审核的入群申请
     */
    private Boolean pending;

    /**
     * 群状态: normal-正常, banned-已封禁
     */
    private String status;
}
