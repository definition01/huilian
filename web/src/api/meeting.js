import request from './request'

export function createInstantMeeting(data) {
  return request.post('/meeting/instant', data)
}

export function createScheduledMeeting(data) {
  return request.post('/meeting/schedule', data)
}

export function getMeetingDetail(meetingId) {
  return request.get(`/meeting/${meetingId}`)
}

export function getMeetingByNumber(meetingNumber) {
  return request.get(`/meeting/number/${meetingNumber}`)
}

export function joinMeeting(data) {
  return request.post('/meeting/join', data)
}

export function leaveMeeting(meetingId) {
  return request.post(`/meeting/${meetingId}/leave`)
}

export function startMeeting(meetingId) {
  return request.post(`/meeting/${meetingId}/start`)
}

export function endMeeting(meetingId) {
  return request.post(`/meeting/${meetingId}/end`)
}

export function cancelMeeting(meetingId) {
  return request.delete(`/meeting/${meetingId}`)
}

export function getMyMeetings(page = 1, size = 10, status) {
  return request.get('/meeting/my', { params: { current: page, size, status } })
}

export function getMeetingList(page = 1, size = 10, status) {
  return request.get('/meeting/list', { params: { current: page, size, status } })
}

export function getParticipants(meetingId) {
  return request.get(`/meeting/${meetingId}/participants`)
}

export function inviteParticipant(meetingId, data) {
  return request.post(`/meeting/${meetingId}/participants`, data)
}

export function removeParticipant(meetingId, userId) {
  return request.delete(`/meeting/${meetingId}/participants/${userId}`)
}

export function muteParticipant(meetingId, userId, muted) {
  return request.put(`/meeting/${meetingId}/participants/${userId}/mute`, null, {
    params: { muted }
  })
}
