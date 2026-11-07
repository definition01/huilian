package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建群聊 DTO
 *
 * @author heathcetide
 */
@Data
public class CreateGroupDTO {

    /**
     * 群名称
     */
    @NotBlank(message = "群名称不能为空")
    @Size(max = 100, message = "群名称不能超过100个字符")
    private String groupName;

    /**
     * 初始成员用户名（群主会自动加入）
     */
    private List<String> memberUsernames;
}
