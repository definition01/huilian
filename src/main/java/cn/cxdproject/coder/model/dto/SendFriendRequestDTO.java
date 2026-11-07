package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发送好友申请 DTO
 *
 * @author heathcetide
 */
@Data
public class SendFriendRequestDTO {

    /**
     * 接收人用户ID
     */
    @NotNull(message = "请选择要添加的用户")
    private Long toUserId;

    /**
     * 申请附言
     */
    @Size(max = 100, message = "申请附言不能超过100个字符")
    private String applyMessage;
}
