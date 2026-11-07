package cn.cxdproject.coder.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("meeting_chat_message")
@EqualsAndHashCode(callSuper = true)
public class MessageSend extends BaseEntity implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会议ID
     */
    @TableField("meeting_id")
    private Long meetingId;

    /**
     * 发送者参会人ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 发送者参会人名称
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 消息类型: text/system/file
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 目标范围: all/private/host
     */
    @TableField("target_scope")
    private String targetScope;

    /**
     * 目标id
     */
    @TableField("receved_id")
    private Long recevedId;

    /**
     * 发送时间
     */
    @TableField("sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    /**
     * 引用消息id
     */
    @TableField("reply_to_id")
    private Long replyToId;

    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            // This should never happen since we implement Cloneable
            throw new RuntimeException("Failed to clone User object", e);
        }
    }
}
