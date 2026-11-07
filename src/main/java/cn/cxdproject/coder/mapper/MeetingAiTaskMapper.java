package cn.cxdproject.coder.mapper;

import cn.cxdproject.coder.model.entity.MeetingAiTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议 AI 异步任务 Mapper
 *
 * @author heathcetide
 */
@Mapper
public interface MeetingAiTaskMapper extends BaseMapper<MeetingAiTask> {
}
