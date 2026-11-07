package cn.cxdproject.coder.service.ai;

import cn.cxdproject.coder.model.dto.MeetingAiSettingsDTO;
import cn.cxdproject.coder.model.entity.MeetingAiSettings;
import cn.cxdproject.coder.model.vo.MeetingAiSettingsVO;

/**
 * 会议 AI 配置服务
 *
 * @author heathcetide
 */
public interface MeetingAiSettingsService {

    /**
     * 获取会议 AI 配置，不存在时返回默认配置（不落库）
     */
    MeetingAiSettings getOrDefault(Long meetingId);

    /**
     * 保存（新增或更新）会议 AI 配置
     */
    MeetingAiSettings saveSettings(Long meetingId, MeetingAiSettingsDTO dto);

    /**
     * 实体转展示对象（解析目标语言 JSON）
     */
    MeetingAiSettingsVO toVO(MeetingAiSettings settings);
}
