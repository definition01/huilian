package cn.cxdproject.coder.mapper;

import cn.cxdproject.coder.model.entity.ImChatState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话已读与置顶状态 Mapper
 *
 * @author heathcetide
 */
@Mapper
public interface ImChatStateMapper extends BaseMapper<ImChatState> {
}
