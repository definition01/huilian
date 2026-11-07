package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 群聊封禁申诉 DTO
 *
 * @author heathcetide
 */
@Data
public class GroupAppealDTO {

    /**
     * 申诉理由
     */
    @NotBlank(message = "申诉理由不能为空")
    @Size(max = 500, message = "申诉理由不能超过500字")
    private String reason;
}
