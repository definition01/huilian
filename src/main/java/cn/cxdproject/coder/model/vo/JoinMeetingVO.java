package cn.cxdproject.coder.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入会议响应 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinMeetingVO {

    /**
     * 会议信息
     */
    private MeetingVO meeting;

    /**
     * Netty服务器地址
     */
    private String nettyHost;

    /**
     * Netty服务器端口
     */
    private Integer nettyPort;

    /**
     * 认证token（用于连接Netty）
     */
    private String authToken;

    /**
     * 当前用户在会议中的角色
     */
    private String role;
}
