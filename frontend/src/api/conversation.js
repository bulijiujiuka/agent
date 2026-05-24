import request from './request'

export function getConversationList(userId) {
  return request({
    url: '/ai/conversation/list',
    method: 'get',
    params: { userId }
  })
}

export function getConversationById(id) {
  return request({
    url: `/ai/conversation/${id}`,
    method: 'get'
  })
}

export function createConversation(data) {
  return request({
    url: '/ai/conversation',
    method: 'post',
    data
  })
}

export function updateConversation(data) {
  return request({
    url: '/ai/conversation',
    method: 'put',
    data
  })
}

export function deleteConversation(id) {
  return request({
    url: `/ai/conversation/${id}`,
    method: 'delete'
  })
}

export function getMessages(conversationId) {
  return request({
    url: `/ai/conversation/${conversationId}/messages`,
    method: 'get'
  })
}

export function sendMessage(conversationId, data) {
  return request({
    url: `/ai/conversation/${conversationId}/messages`,
    method: 'post',
    data
  })
}

export function aiChat(conversationId, question) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data: { conversationId, question },
    timeout: 60000
  })
}

export function indexKnowledge() {
  return request({
    url: '/ai/chat/index',
    method: 'post',
    timeout: 120000
  })
}
