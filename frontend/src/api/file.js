import request from './request'

/**
 * 单文件上传
 * @param {File} file - 文件对象
 * @param {string} bizType - 业务类型（avatar/post/resource等）
 * @param {string} uploadUser - 上传用户
 * @param {Function} onProgress - 上传进度回调
 */
export function uploadFile(file, bizType, uploadUser, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  if (bizType) formData.append('bizType', bizType)
  if (uploadUser) formData.append('uploadUser', uploadUser)

  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
      ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
      : undefined
  })
}

/**
 * 批量上传
 */
export function uploadFiles(files, bizType, uploadUser) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  if (bizType) formData.append('bizType', bizType)
  if (uploadUser) formData.append('uploadUser', uploadUser)

  return request({
    url: '/file/upload/batch',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取文件信息
 */
export function getFileInfo(id) {
  return request({ url: `/file/${id}`, method: 'get' })
}

/**
 * 获取文件列表
 */
export function getFileList(bizType) {
  return request({
    url: '/file/list',
    method: 'get',
    params: bizType ? { bizType } : {}
  })
}

/**
 * 删除文件
 */
export function deleteFile(id) {
  return request({ url: `/file/${id}`, method: 'delete' })
}

/**
 * 获取文件下载URL
 */
export function getDownloadUrl(id) {
  return `/api/file/download/${id}` // 这里是直接浏览器访问，需要完整路径
}

/**
 * 获取文件预览URL（图片/PDF等可直接访问的文件）
 */
export function getPreviewUrl(fileUrl) {
  return fileUrl
}
