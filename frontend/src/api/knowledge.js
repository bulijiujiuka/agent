import request from './request'

export function getDocumentList() {
  return request({
    url: '/kb/document/list',
    method: 'get'
  })
}

export function getDocumentPage(params) {
  return request({
    url: '/kb/document/page',
    method: 'get',
    params
  })
}

export function getDocumentById(id) {
  return request({
    url: `/kb/document/${id}`,
    method: 'get'
  })
}

export function createDocument(data) {
  return request({
    url: '/kb/document',
    method: 'post',
    data
  })
}

export function updateDocument(data) {
  return request({
    url: '/kb/document',
    method: 'put',
    data
  })
}

export function deleteDocument(id) {
  return request({
    url: `/kb/document/${id}`,
    method: 'delete'
  })
}

export function getDocumentChunks(id) {
  return request({
    url: `/kb/document/${id}/chunks`,
    method: 'get'
  })
}

export function toggleDocumentEnabled(id, enabled) {
  return request({
    url: `/kb/document/${id}/enabled`,
    method: 'put',
    data: { enabled }
  })
}

export function getDocumentVersions(id) {
  return request({
    url: `/kb/document/${id}/versions`,
    method: 'get'
  })
}

export function rollbackDocumentVersion(id, version) {
  return request({
    url: `/kb/document/${id}/rollback/${version}`,
    method: 'post'
  })
}

export function uploadDocument(formData) {
  return request({
    url: '/kb/document/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function reuploadDocument(id, formData) {
  return request({
    url: `/kb/document/${id}/reupload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function getVersionContent(id, version) {
  return request({
    url: `/kb/document/${id}/versions/${version}/content`,
    method: 'get'
  })
}

export function getDocumentContent(id) {
  return request({
    url: `/kb/document/${id}/content`,
    method: 'get'
  })
}
