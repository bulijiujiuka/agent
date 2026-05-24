<template>
  <div class="file-container">
    <!-- 上传区域 -->
    <el-card>
      <template #header>
        <span>文件上传测试</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 按钮上传 -->
        <el-tab-pane label="按钮上传" name="button">
          <FileUpload
            v-model="buttonFiles"
            biz-type="test"
            button-text="选择文件"
            multiple
            :limit="5"
            @success="onUploadSuccess"
          />
        </el-tab-pane>

        <!-- 拖拽上传 -->
        <el-tab-pane label="拖拽上传" name="drag">
          <FileUpload
            v-model="dragFiles"
            biz-type="test"
            drag
            multiple
            @success="onUploadSuccess"
          />
        </el-tab-pane>

        <!-- 图片上传 -->
        <el-tab-pane label="图片上传" name="image">
          <FileUpload
            v-model="imageFiles"
            biz-type="avatar"
            list-type="picture-card"
            :accept="['jpg', 'jpeg', 'png', 'gif', 'webp']"
            :max-size-m-b="10"
            :limit="3"
            multiple
            @success="onUploadSuccess"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 已上传文件列表 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>已上传文件列表</span>
          <el-button type="primary" :icon="Refresh" @click="fetchFiles">刷新</el-button>
        </div>
      </template>

      <el-table :data="serverFiles" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileExt" label="类型" width="80" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务类型" width="100" />
        <el-table-column prop="uploadUser" label="上传用户" width="120" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <el-image
              v-if="isImage(row.fileExt)"
              :src="row.fileUrl"
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 4px; cursor: pointer;"
              @click="handlePreview(row)"
            />
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="700px" destroy-on-close>
      <div style="text-align: center;">
        <el-image :src="previewUrl" fit="contain" style="max-width: 100%; max-height: 70vh;" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FileUpload from '@/components/FileUpload/index.vue'
import { getFileList, deleteFile, getDownloadUrl } from '@/api/file'

const activeTab = ref('button')
const buttonFiles = ref([])
const dragFiles = ref([])
const imageFiles = ref([])
const serverFiles = ref([])
const loading = ref(false)

const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await getFileList()
    serverFiles.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const onUploadSuccess = () => {
  fetchFiles()
}

const handleDownload = (row) => {
  window.open(getDownloadUrl(row.id), '_blank')
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除文件「${row.originalName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteFile(row.id)
      ElMessage.success('删除成功')
      fetchFiles()
    })
}

const previewVisible = ref(false)
const previewUrl = ref('')

const handlePreview = (row) => {
  previewUrl.value = row.fileUrl
  previewVisible.value = true
}

const isImage = (ext) => ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext?.toLowerCase())

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

onMounted(() => {
  fetchFiles()
})
</script>

<style lang="scss" scoped>
.file-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
