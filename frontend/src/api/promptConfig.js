import request from './request'

export function getPromptConfigList() {
  return request({
    url: '/ai/prompt-config',
    method: 'get'
  })
}

export function getPromptConfigById(id) {
  return request({
    url: `/ai/prompt-config/${id}`,
    method: 'get'
  })
}

export function createPromptConfig(data) {
  return request({
    url: '/ai/prompt-config',
    method: 'post',
    data
  })
}

export function updatePromptConfig(data) {
  return request({
    url: '/ai/prompt-config',
    method: 'put',
    data
  })
}

export function deletePromptConfig(id) {
  return request({
    url: `/ai/prompt-config/${id}`,
    method: 'delete'
  })
}
