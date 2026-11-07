package cn.cxdproject.coder.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 加入会议 DTO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinMeetingDTO {

    /**
     * 会议号
     */
    @NotBlank(message = "会议号不能为空")
    private String meetingNumber;

    /**
     * 入会密码（如果会议设置了密码）
     */
    @Size(min = 6, max = 6, message = "密码必须是6位")
    private String password;
}
