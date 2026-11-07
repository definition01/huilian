package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新用户信息DTO
 * 
 * @author heathcetide
 */
@Data
public class UpdateUserDTO {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 手机号（仅支持 +86 中国手机号，可带 +86 前缀）
     */
    @Pattern(regexp = "^(?:\\+86)?1[3-9]\\d{9}$", message = "手机号格式不正确，仅支持 +86 中国手机号")
    private String mobile;

    /**
     * 邮箱
     */
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;
    
    /**
     * 头像
     */
    private String avatar;

}

