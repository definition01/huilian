package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 好友备注 DTO
 *
 * @author heathcetide
 */
@Data
public class FriendRemarkDTO {

    /**
     * 备注，留空表示取消备注
     */
    @Size(max = 50, message = "备注不能超过50个字符")
    private String remark;
}
