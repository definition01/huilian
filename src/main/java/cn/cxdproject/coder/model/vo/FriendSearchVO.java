package cn.cxdproject.coder.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友搜索结果 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendSearchVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号（脱敏）
     */
    private String mobile;

    /**
     * 与当前用户的关系: NONE/FRIEND/REQUEST_SENT/REQUEST_RECEIVED
     */
    private String relation;
}
