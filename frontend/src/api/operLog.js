import request from './request'

export function getOperLogPage(params) {
  return request({
    url: '/oper-log/page',
    method: 'get',
    params
  })
}

export function deleteOperLog(id) {
  return request({
    url: `/oper-log/${id}`,
    method: 'delete'
  })
}

export function clearOperLog() {
  return request({
    url: '/oper-log/clear',
    method: 'delete'
  })
}
