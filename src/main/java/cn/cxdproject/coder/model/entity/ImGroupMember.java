package cn.cxdproject.coder.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ImGroupMember 实体类
 * 群成员信息
 *
 * @author heathcetide
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("im_group_member")
@EqualsAndHashCode(callSuper = true)
public class ImGroupMember extends BaseEntity implements Serializable, Cloneable {

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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色: owner/admin/member
     */
    @TableField("role_type")
    private String roleType;

    /**
     * 加入时间
     */
    @TableField("join_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    @Override
    public ImGroupMember clone() {
        try {
            return (ImGroupMember) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ImGroupMember object", e);
        }
    }
}
