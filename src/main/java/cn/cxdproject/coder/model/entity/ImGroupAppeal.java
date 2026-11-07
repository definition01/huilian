package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群聊封禁申诉
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_group_appeal")
@EqualsAndHashCode(callSuper = true)
public class ImGroupAppeal extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 申诉人（群主）用户ID
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 申诉理由
     */
    @TableField("reason")
    private String reason;

    /**
     * 状态: PENDING/APPROVED/REJECTED
     */
    @TableField("status")
    private String status;

    /**
     * 管理员处理备注
     */
    @TableField("admin_remark")
    private String adminRemark;

    @Override
    public ImGroupAppeal clone() {
        try {
            return (ImGroupAppeal) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImGroupAppeal object", e);
        }
    }
}
