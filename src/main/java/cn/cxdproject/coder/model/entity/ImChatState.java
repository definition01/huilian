package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话已读与置顶状态实体
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_chat_state")
@EqualsAndHashCode(callSuper = true)
public class ImChatState extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话类型: GROUP/PRIVATE
     */
    @TableField("conversation_type")
    private String conversationType;

    /**
     * 会话目标ID（群ID或好友用户ID）
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 已读到的最后一条消息ID
     */
    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    /**
     * 是否置顶
     */
    @TableField("pinned")
    private Boolean pinned;

    @Override
    public ImChatState clone() {
        try {
            return (ImChatState) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImChatState object", e);
        }
    }
}
