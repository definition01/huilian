package cn.cxdproject.coder.mapper;

import cn.cxdproject.coder.model.entity.MeetingTranscript;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议语音转文字记录 Mapper
 *
 * @author heathcetide
 */
@Mapper
public interface MeetingTranscriptMapper extends BaseMapper<MeetingTranscript> {
}
