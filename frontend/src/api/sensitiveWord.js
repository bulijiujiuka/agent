import request from './request'

export function getSensitiveWordList() {
  return request({ url: '/sensitive-word', method: 'get' })
}

export function createSensitiveWord(data) {
  return request({ url: '/sensitive-word', method: 'post', data })
}

export function updateSensitiveWord(data) {
  return request({ url: '/sensitive-word', method: 'put', data })
}

export function deleteSensitiveWord(id) {
  return request({ url: `/sensitive-word/${id}`, method: 'delete' })
}
