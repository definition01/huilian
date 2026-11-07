package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 私聊消息实体
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_private_message")
@EqualsAndHashCode(callSuper = true)
public class ImPrivateMessage extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者用户ID
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收者用户ID
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 消息类型: TEXT
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    @Override
    public ImPrivateMessage clone() {
        try {
            return (ImPrivateMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImPrivateMessage object", e);
        }
    }
}
