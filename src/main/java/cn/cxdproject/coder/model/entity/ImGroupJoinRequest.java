package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群聊入群申请实体
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_group_join_request")
@EqualsAndHashCode(callSuper = true)
public class ImGroupJoinRequest extends BaseEntity implements Serializable, Cloneable {

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
     * 申请人用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 邀请人用户ID，NULL 表示用户主动申请
     */
    @TableField("inviter_id")
    private Long inviterId;

    /**
     * 状态: PENDING/APPROVED/REJECTED
     */
    @TableField("status")
    private String status;

    @Override
    public ImGroupJoinRequest clone() {
        try {
            return (ImGroupJoinRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImGroupJoinRequest object", e);
        }
    }
}
