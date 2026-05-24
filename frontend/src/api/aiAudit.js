import request from './request'

export function getAuditConversations(params) {
  return request({ url: '/ai/audit/conversations', method: 'get', params })
}

export function getAuditMessages(conversationId) {
  return request({ url: `/ai/audit/conversations/${conversationId}/messages`, method: 'get' })
}

export function getCallLogs(params) {
  return request({ url: '/ai/audit/call-logs', method: 'get', params })
}

export function getCallLogTrace(traceId) {
  return request({ url: `/ai/audit/call-logs/trace/${traceId}`, method: 'get' })
}

export function getCallLogStats(days = 7) {
  return request({ url: '/ai/audit/call-logs/stats', method: 'get', params: { days } })
}

export function getRetrievalEval() {
  return request({ url: '/ai/audit/retrieval-eval', method: 'get' })
}
