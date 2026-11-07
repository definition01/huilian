package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 邀请好友入群 DTO
 *
 * @author heathcetide
 */
@Data
public class InviteGroupMembersDTO {

    /**
     * 待邀请好友的用户ID
     */
    @NotEmpty(message = "请选择要邀请的好友")
    private List<Long> userIds;
}
