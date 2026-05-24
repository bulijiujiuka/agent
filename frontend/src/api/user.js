import request from './request'

export function getUserList() {
  return request({
    url: '/user/list',
    method: 'get'
  })
}

export function getUserPage(params) {
  return request({
    url: '/user/page',
    method: 'get',
    params
  })
}

export function createUser(data) {
  return request({
    url: '/user',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/user',
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  })
}

export function getUserRoles(id) {
  return request({
    url: `/user/${id}/roles`,
    method: 'get'
  })
}

export function assignUserRoles(id, roleIds) {
  return request({
    url: `/user/${id}/roles`,
    method: 'post',
    data: roleIds
  })
}

export function resetUserPassword(id, newPassword) {
  return request({
    url: `/user/${id}/resetPassword`,
    method: 'put',
    data: { newPassword }
  })
}
