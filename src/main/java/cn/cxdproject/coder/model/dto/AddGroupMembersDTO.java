package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 添加群成员 DTO
 *
 * @author heathcetide
 */
@Data
public class AddGroupMembersDTO {

    /**
     * 待添加成员的用户名
     */
    @NotEmpty(message = "请选择要添加的成员")
    private List<String> memberUsernames;
}
