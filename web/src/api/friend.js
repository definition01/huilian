import request from './request'

export function searchUsers(keyword) {
  return request.get('/friend/search', { params: { keyword } })
}

export function sendFriendRequest(data) {
  return request.post('/friend/requests', data)
}

export function getReceivedFriendRequests() {
  return request.get('/friend/requests/received')
}

export function getSentFriendRequests() {
  return request.get('/friend/requests/sent')
}

export function approveFriendRequest(requestId) {
  return request.post(`/friend/requests/${requestId}/approve`)
}

export function rejectFriendRequest(requestId) {
  return request.post(`/friend/requests/${requestId}/reject`)
}

export function getFriends() {
  return request.get('/friend/list')
}

export function updateFriendRemark(friendId, remark) {
  return request.put(`/friend/${friendId}/remark`, { remark })
}

export function getPrivateMessages(friendId, params = {}) {
  return request.get(`/friend/${friendId}/messages`, { params })
}

export function sendPrivateMessage(friendId, data) {
  return request.post(`/friend/${friendId}/messages`, data)
}
