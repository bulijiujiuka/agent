<template>
  <div class="file-upload">
    <el-upload
      ref="uploadRef"
      :action="uploadAction"
      :headers="uploadHeaders"
      :data="uploadData"
      :accept="acceptTypes"
      :limit="limit"
      :multiple="multiple"
      :drag="drag"
      :show-file-list="showFileList"
      :file-list="fileList"
      :list-type="listType"
      :auto-upload="autoUpload"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
    >
      <!-- 拖拽模式 -->
      <template v-if="drag">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
      </template>

      <!-- 图片模式 -->
      <template v-else-if="listType === 'picture-card'">
        <el-icon><Plus /></el-icon>
      </template>

      <!-- 按钮模式（默认） -->
      <template v-else>
        <el-button type="primary"><el-icon><UploadFilled /></el-icon> {{ buttonText }}</el-button>
      </template>

      <!-- 提示信息 -->
      <template #tip>
        <div class="el-upload__tip" v-if="showTip">
          支持 {{ acceptLabel }} 格式，单个文件不超过 {{ maxSizeMB }}MB
          <span v-if="limit">，最多上传 {{ limit }} 个文件</span>
        </div>
      </template>
    </el-upload>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="800px" append-to-body>
      <div class="preview-container">
        <img v-if="isImage(previewFile)" :src="previewFile.url" class="preview-image" />
        <iframe v-else-if="isPdf(previewFile)" :src="previewFile.url" class="preview-pdf" />
        <div v-else class="preview-unsupported">
          <el-icon :size="48"><Document /></el-icon>
          <p>{{ previewFile.name }}</p>
          <el-button type="primary" @click="downloadFile(previewFile)">下载文件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { UploadFilled, Plus, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  /** 业务类型（avatar/post/resource等） */
  bizType: { type: String, default: '' },
  /** 上传按钮文字 */
  buttonText: { type: String, default: '上传文件' },
  /** 是否拖拽上传 */
  drag: { type: Boolean, default: false },
  /** 列表类型：text / picture / picture-card */
  listType: { type: String, default: 'text' },
  /** 是否多选 */
  multiple: { type: Boolean, default: false },
  /** 最大上传数量 */
  limit: { type: Number, default: 0 },
  /** 最大文件大小（MB） */
  maxSizeMB: { type: Number, default: 50 },
  /** 允许的文件类型（扩展名数组），为空则不限 */
  accept: { type: Array, default: () => [] },
  /** 是否自动上传 */
  autoUpload: { type: Boolean, default: true },
  /** 是否显示文件列表 */
  showFileList: { type: Boolean, default: true },
  /** 是否显示提示信息 */
  showTip: { type: Boolean, default: true },
  /** 已有文件列表（回显用） */
  modelValue: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'success', 'remove'])

const uploadRef = ref()
const previewVisible = ref(false)
const previewFile = ref({ name: '', url: '' })

const userStore = useUserStore()

// 上传地址
const uploadAction = '/api/file/upload'

// 请求头（带 JWT Token）
const uploadHeaders = computed(() => {
  const token = userStore.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 附加参数
const uploadData = computed(() => {
  const data = {}
  if (props.bizType) data.bizType = props.bizType
  if (userStore.userInfo?.username) data.uploadUser = userStore.userInfo.username
  return data
})

// accept 属性
const acceptTypes = computed(() => {
  if (props.accept.length === 0) return ''
  return props.accept.map(ext => `.${ext}`).join(',')
})

// 提示文本中的格式说明
const acceptLabel = computed(() => {
  if (props.accept.length === 0) return '常见'
  return props.accept.join('、')
})

// 文件列表（双向绑定）
const fileList = computed(() => {
  return props.modelValue.map(f => ({
    name: f.originalName || f.name,
    url: f.fileUrl || f.url,
    id: f.id,
    raw: f
  }))
})

// 上传前校验
const handleBeforeUpload = (rawFile) => {
  // 校验大小
  if (rawFile.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMB}MB`)
    return false
  }

  // 校验扩展名
  if (props.accept.length > 0) {
    const ext = rawFile.name.split('.').pop().toLowerCase()
    if (!props.accept.includes(ext)) {
      ElMessage.error(`不支持的文件类型: .${ext}`)
      return false
    }
  }

  return true
}

// 上传成功
const handleSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    const fileInfo = response.data
    const newList = [...props.modelValue, fileInfo]
    emit('update:modelValue', newList)
    emit('success', fileInfo)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 上传失败
const handleError = () => {
  ElMessage.error('上传失败，请稍后重试')
}

// 超出数量限制
const handleExceed = () => {
  ElMessage.warning(`最多上传 ${props.limit} 个文件`)
}

// 移除文件
const handleRemove = (uploadFile) => {
  const raw = uploadFile.raw || uploadFile
  const newList = props.modelValue.filter(f => {
    if (f.id && raw.id) return f.id !== raw.id
    return f.originalName !== uploadFile.name && f.name !== uploadFile.name
  })
  emit('update:modelValue', newList)
  emit('remove', uploadFile)
}

// 预览文件
const handlePreview = (uploadFile) => {
  previewFile.value = {
    name: uploadFile.name,
    url: uploadFile.url || uploadFile.response?.data?.fileUrl
  }
  previewVisible.value = true
}

// 判断是否为图片
const isImage = (file) => {
  if (!file.url) return false
  return /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(file.url)
}

// 判断是否为 PDF
const isPdf = (file) => {
  if (!file.url) return false
  return /\.pdf$/i.test(file.url)
}

// 下载文件
const downloadFile = (file) => {
  const a = document.createElement('a')
  a.href = file.url
  a.download = file.name
  a.click()
}

// 暴露方法供父组件调用
defineExpose({
  /** 手动触发上传（autoUpload=false 时使用） */
  submit: () => uploadRef.value?.submit(),
  /** 清空文件列表 */
  clearFiles: () => uploadRef.value?.clearFiles()
})
</script>

<style lang="scss" scoped>
.file-upload {
  width: 100%;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.preview-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.preview-pdf {
  width: 100%;
  height: 600px;
  border: none;
}

.preview-unsupported {
  text-align: center;
  color: #909399;

  p {
    margin: 16px 0;
    font-size: 14px;
  }
}
</style>
