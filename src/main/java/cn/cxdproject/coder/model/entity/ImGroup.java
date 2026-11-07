package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * ImGroup 实体类
 * 群聊信息
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_group")
@EqualsAndHashCode(callSuper = true)
public class ImGroup extends BaseEntity implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 群号（10位数字，唯一）
     */
    @TableField("group_no")
    private String groupNo;

    /**
     * 群主用户ID
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 群头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 群公告
     */
    @TableField("announcement")
    private String announcement;

    /**
     * 群状态: normal-正常, banned-已封禁
     */
    @TableField("status")
    private String status;

    @Override
    public ImGroup clone() {
        try {
            return (ImGroup) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImGroup object", e);
        }
    }
}
