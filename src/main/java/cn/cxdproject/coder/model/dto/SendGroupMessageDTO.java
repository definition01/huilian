package cn.cxdproject.coder.model.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 发送群消息 DTO
 *
 * @author heathcetide
 */
@Data
public class SendGroupMessageDTO {

    /**
     * 消息类型: TEXT/IMAGE/FILE，默认 TEXT
     */
    private String messageType;

    /**
     * 消息内容（文本消息必填）
     */
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    private String content;

    /**
     * 附件访问地址（图片/文件消息必填）
     */
    private String fileUrl;

    /**
     * 附件原始名称
     */
    @Size(max = 255, message = "附件名称不能超过255个字符")
    private String fileName;

    /**
     * 附件大小(字节)
     */
    private Long fileSize;
}
