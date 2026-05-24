import request from './request'

export function getTicketList() {
  return request({
    url: '/ticket/list',
    method: 'get'
  })
}

export function getTicketPage(params) {
  return request({
    url: '/ticket/page',
    method: 'get',
    params
  })
}

export function getTicketById(id) {
  return request({
    url: `/ticket/${id}`,
    method: 'get'
  })
}

export function createTicket(data) {
  return request({
    url: '/ticket',
    method: 'post',
    data
  })
}

export function updateTicket(data) {
  return request({
    url: '/ticket',
    method: 'put',
    data
  })
}

export function deleteTicket(id) {
  return request({
    url: `/ticket/${id}`,
    method: 'delete'
  })
}

export function regenerateAiAssist(id) {
  return request({
    url: `/ticket/${id}/ai-assist`,
    method: 'post',
    timeout: 60000
  })
}
