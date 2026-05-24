import request from './request'

export function getModelConfigList() {
  return request({ url: '/ai/model-config', method: 'get' })
}

export function getEnabledModels() {
  return request({ url: '/ai/model-config/enabled', method: 'get' })
}

export function createModelConfig(data) {
  return request({ url: '/ai/model-config', method: 'post', data })
}

export function updateModelConfig(data) {
  return request({ url: '/ai/model-config', method: 'put', data })
}

export function deleteModelConfig(id) {
  return request({ url: `/ai/model-config/${id}`, method: 'delete' })
}
