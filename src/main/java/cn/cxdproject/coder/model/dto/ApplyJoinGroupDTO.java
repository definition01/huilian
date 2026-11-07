package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 通过群号申请入群 DTO
 *
 * @author heathcetide
 */
@Data
public class ApplyJoinGroupDTO {

    /**
     * 群号（10位数字）
     */
    @NotBlank(message = "群号不能为空")
    @Pattern(regexp = "^\\d{10}$", message = "群号必须是10位数字")
    private String groupNo;
}
