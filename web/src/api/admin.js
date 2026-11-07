import request from './request'

export function getAdminStats() {
  return request.get('/admin/stats')
}

export function getAdminMeetings(params) {
  return request.get('/admin/meetings', { params })
}

export function deleteAdminMeeting(meetingId) {
  return request.delete(`/admin/meetings/${meetingId}`)
}

export function getAdminMeetingSummary(meetingId) {
  return request.get(`/admin/meetings/${meetingId}/summary`)
}

export function getAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateAdminUserStatus(userId, enabled) {
  return request.put(`/admin/users/${userId}/status`, null, { params: { enabled } })
}

export function getAdminGroups(params) {
  return request.get('/admin/groups', { params })
}

export function deleteAdminGroup(groupId) {
  return request.delete(`/admin/groups/${groupId}`)
}

export function updateAdminGroupStatus(groupId, banned) {
  return request.put(`/admin/groups/${groupId}/status`, null, { params: { banned } })
}

export function joinAdminGroup(groupId) {
  return request.post(`/admin/groups/${groupId}/join`)
}

export function getAdminGroupAppeals(params) {
  return request.get('/admin/group-appeals', { params })
}

export function reviewAdminGroupAppeal(appealId, approve, adminRemark) {
  return request.put(`/admin/group-appeals/${appealId}/review`, null, {
    params: { approve, adminRemark }
  })
}
