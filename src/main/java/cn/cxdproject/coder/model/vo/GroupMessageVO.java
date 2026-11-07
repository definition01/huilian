package cn.cxdproject.coder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群消息 VO
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessageVO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 群ID
     */
    private Long groupId;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息类型: TEXT/SYSTEM
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附件访问地址
     */
    private String fileUrl;

    /**
     * 附件原始名称
     */
    private String fileName;

    /**
     * 附件大小(字节)
     */
    private Long fileSize;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
