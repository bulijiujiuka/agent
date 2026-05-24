import request from './request'

export function getCategoryList() {
  return request({ url: '/kb/category', method: 'get' })
}

export function createCategory(data) {
  return request({ url: '/kb/category', method: 'post', data })
}

export function updateCategory(data) {
  return request({ url: '/kb/category', method: 'put', data })
}

export function deleteCategory(id) {
  return request({ url: `/kb/category/${id}`, method: 'delete' })
}
