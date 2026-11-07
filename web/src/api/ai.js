import request from './request'

/**
 * 获取会议 AI 配置
 */
export function getAiSettings(meetingId) {
  return request.get(`/ai/meeting/${meetingId}/settings`)
}

/**
 * 更新会议 AI 配置（仅主持人）
 */
export function updateAiSettings(meetingId, data) {
  return request.put(`/ai/meeting/${meetingId}/settings`, data)
}

/**
 * 查询会议实时翻译记录
 */
export function getTranslations(meetingId, targetLanguage) {
  return request.get(`/ai/meeting/${meetingId}/translations`, {
    params: targetLanguage ? { targetLanguage } : {}
  })
}

/**
 * 查询会议最近一次 AI 总结
 */
export function getMeetingSummary(meetingId) {
  return request.get(`/ai/meeting/${meetingId}/summary`)
}

/**
 * 手动触发生成 AI 会议总结（仅主持人）
 */
export function triggerMeetingSummary(meetingId) {
  return request.post(`/ai/meeting/${meetingId}/summary`)
}
