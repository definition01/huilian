package cn.cxdproject.coder.service.impl;

import cn.cxdproject.coder.mapper.MeetingTranscriptMapper;
import cn.cxdproject.coder.model.entity.MeetingTranscript;
import cn.cxdproject.coder.service.MeetingTranscriptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议语音转文字记录 Service 实现
 *
 * @author heathcetide
 */
@Slf4j
@Service
public class MeetingTranscriptServiceImpl extends ServiceImpl<MeetingTranscriptMapper, MeetingTranscript>
        implements MeetingTranscriptService {

    @Override
    public void saveTranscript(MeetingTranscript transcript) {
        if (transcript.getCreatedAt() == null) {
            transcript.setCreatedAt(LocalDateTime.now());
        }
        if (transcript.getUpdatedAt() == null) {
            transcript.setUpdatedAt(LocalDateTime.now());
        }
        save(transcript);
        log.debug("保存会议转录记录 - 会议ID: {}, 用户: {}, 内容: {}",
                transcript.getMeetingId(), transcript.getUserName(), transcript.getTextContent());
    }

    @Override
    public List<MeetingTranscript> getTranscriptsByMeetingId(Long meetingId) {
        return list(new LambdaQueryWrapper<MeetingTranscript>()
                .eq(MeetingTranscript::getMeetingId, meetingId)
                .orderByAsc(MeetingTranscript::getStartTime));
    }

    @Override
    public List<MeetingTranscript> getFinalTranscriptsByMeetingId(Long meetingId) {
        return list(new LambdaQueryWrapper<MeetingTranscript>()
                .eq(MeetingTranscript::getMeetingId, meetingId)
                .eq(MeetingTranscript::getIsFinal, true)
                .orderByAsc(MeetingTranscript::getStartTime));
    }

    @Override
    public void deleteByMeetingId(Long meetingId) {
        remove(new LambdaQueryWrapper<MeetingTranscript>()
                .eq(MeetingTranscript::getMeetingId, meetingId));
        log.info("删除会议转录记录 - 会议ID: {}", meetingId);
    }
}
