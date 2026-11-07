package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 好友申请实体
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_friend_request")
@EqualsAndHashCode(callSuper = true)
public class ImFriendRequest extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人用户ID
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收人用户ID
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 申请附言
     */
    @TableField("apply_message")
    private String applyMessage;

    /**
     * 状态: PENDING/APPROVED/REJECTED
     */
    @TableField("status")
    private String status;

    @Override
    public ImFriendRequest clone() {
        try {
            return (ImFriendRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImFriendRequest object", e);
        }
    }
}
