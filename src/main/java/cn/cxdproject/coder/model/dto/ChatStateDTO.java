package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 会话已读/置顶请求 DTO
 *
 * @author heathcetide
 */
@Data
public class ChatStateDTO {

    /**
     * 会话类型: GROUP/PRIVATE
     */
    @NotBlank(message = "会话类型不能为空")
    @Pattern(regexp = "^(GROUP|PRIVATE)$", message = "会话类型只能是 GROUP 或 PRIVATE")
    private String type;

    /**
     * 会话目标ID（群ID或好友用户ID）
     */
    @NotNull(message = "会话目标ID不能为空")
    private Long targetId;

    /**
     * 是否置顶（仅置顶接口使用）
     */
    private Boolean pinned;
}
