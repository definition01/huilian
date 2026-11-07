import request from './request'

export function markChatRead(data) {
  return request.post('/chat/read', data)
}

export function pinChat(data) {
  return request.post('/chat/pin', data)
}
