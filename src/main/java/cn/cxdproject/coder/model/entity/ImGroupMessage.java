package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * ImGroupMessage 实体类
 * 群聊消息
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_group_message")
@EqualsAndHashCode(callSuper = true)
public class ImGroupMessage extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 发送者用户ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 发送者名称
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 消息类型: TEXT/SYSTEM
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 附件访问地址
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 附件原始名称
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 附件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;

    @Override
    public ImGroupMessage clone() {
        try {
            return (ImGroupMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImGroupMessage object", e);
        }
    }
}
