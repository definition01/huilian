import request from './request'

/**
 * 获取会议转录记录
 */
export function getTranscripts(meetingId, finalOnly = false) {
  return request({
    url: `/transcript/${meetingId}`,
    method: 'get',
    params: { finalOnly }
  })
}

/**
 * 删除会议转录记录
 */
export function deleteTranscripts(meetingId) {
  return request({
    url: `/transcript/${meetingId}`,
    method: 'delete'
  })
}

/**
 * 启动ASR
 */
export function startAsr(meetingId, userId, userName) {
  return request({
    url: '/transcript/start',
    method: 'post',
    params: { meetingId, userId, userName }
  })
}

/**
 * 停止ASR
 */
export function stopAsr(meetingId, userId) {
  return request({
    url: '/transcript/stop',
    method: 'post',
    params: { meetingId, userId }
  })
}
