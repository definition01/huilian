import request from './request'

export function createGroup(data) {
  return request.post('/group', data)
}

export function getMyGroups() {
  return request.get('/group/list')
}

export function getGroupDetail(groupId) {
  return request.get(`/group/${groupId}`)
}

export function getGroupMembers(groupId) {
  return request.get(`/group/${groupId}/members`)
}

export function inviteGroupMembers(groupId, data) {
  return request.post(`/group/${groupId}/invite`, data)
}

export function removeGroupMember(groupId, userId) {
  return request.delete(`/group/${groupId}/members/${userId}`)
}

export function dissolveGroup(groupId) {
  return request.delete(`/group/${groupId}`)
}

export function leaveGroup(groupId) {
  return request.post(`/group/${groupId}/leave`)
}

export function getGroupMessages(groupId, params = {}) {
  return request.get(`/group/${groupId}/messages`, { params })
}

export function sendGroupMessage(groupId, data) {
  return request.post(`/group/${groupId}/messages`, data)
}

export function uploadGroupFile(groupId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/group/${groupId}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export function searchGroupByNo(groupNo) {
  return request.get('/group/search', { params: { groupNo } })
}

export function applyJoinGroup(data) {
  return request.post('/group/apply', data)
}

export function getJoinRequests(groupId) {
  return request.get(`/group/${groupId}/join-requests`)
}

export function approveJoinRequest(groupId, requestId) {
  return request.post(`/group/${groupId}/join-requests/${requestId}/approve`)
}

export function rejectJoinRequest(groupId, requestId) {
  return request.post(`/group/${groupId}/join-requests/${requestId}/reject`)
}

export function submitGroupAppeal(groupId, data) {
  return request.post(`/group/${groupId}/appeal`, data)
}

export function getGroupAppeal(groupId) {
  return request.get(`/group/${groupId}/appeal`)
}
