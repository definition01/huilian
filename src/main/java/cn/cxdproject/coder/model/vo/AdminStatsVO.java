package cn.cxdproject.coder.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端数据概览 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsVO {

    /**
     * 用户总数
     */
    private Long totalUsers;

    /**
     * 启用用户数
     */
    private Long activeUsers;

    /**
     * 禁用用户数
     */
    private Long disabledUsers;

    /**
     * 会议总数
     */
    private Long totalMeetings;

    /**
     * 进行中会议数
     */
    private Long ongoingMeetings;

    /**
     * 群聊总数
     */
    private Long totalGroups;

    /**
     * 群成员总数
     */
    private Long totalGroupMembers;
}
