<template>
  <div class="knowledge-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识库管理</span>
          <div>
            <el-button type="success" :icon="Upload" @click="uploadVisible = true">上传文档</el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增文档</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="文档名称">
          <el-input v-model="searchForm.documentName" placeholder="请输入文档名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width:150px">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析状态">
          <el-select v-model="searchForm.parseStatus" placeholder="请选择" clearable style="width: 130px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="documentName" label="文档名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="sourceType" label="来源" width="100">
          <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.parseStatus)" size="small">{{ statusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="embeddingStatus" label="向量化" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.embeddingStatus)" size="small">{{ statusLabel(row.embeddingStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="80" />
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="(val) => handleToggleEnabled(row, val)" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column prop="uploadedBy" label="上传人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleVersions(row)">版本</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
      />
    </el-card>

    <el-dialog v-model="uploadVisible" title="上传知识文档" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="uploadCategory" placeholder="选择分类（可选）" clearable style="width:100%">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".txt,.md,.csv,.pdf,.doc,.docx"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只能上传一个文件')"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 txt、md、csv 等文本文件，解析后自动切分并索引到向量库</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="文档名称" prop="documentName">
          <el-input v-model="form.documentName" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" clearable style="width:100%">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option label="手动上传" value="MANUAL" />
            <el-option label="系统导入" value="IMPORT" />
            <el-option label="爬虫采集" value="CRAWL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="替换文件">
          <el-upload
            ref="editUploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleEditFileChange"
            :on-remove="() => editFile = null"
            accept=".txt,.md,.csv,.json,.pdf,.doc,.docx"
          >
            <el-button type="primary" link><el-icon><Upload /></el-icon> 选择新文件（可选）</el-button>
          </el-upload>
          <div style="color:#909399;font-size:12px;margin-top:4px">选择文件后将重新解析文档内容并更新知识库索引</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionVisible" title="版本历史" width="750px" destroy-on-close>
      <el-table :data="versionList" v-loading="versionLoading" stripe>
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column prop="documentName" label="文档名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="createTime" label="快照时间" width="170" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreviewVersion(row)">查看</el-button>
            <el-button type="success" link @click="handleDownloadVersion(row)">下载</el-button>
            <el-button type="warning" link @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="700px" destroy-on-close>
      <div style="max-height:500px;overflow-y:auto;padding:12px;background:#f5f7fa;border-radius:6px;">
        <pre style="white-space:pre-wrap;word-break:break-all;font-size:14px;line-height:1.8;margin:0;">{{ previewContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus, Search, Refresh, Edit, Delete, Upload } from '@element-plus/icons-vue'
import { getDocumentPage, createDocument, updateDocument, deleteDocument, uploadDocument, reuploadDocument, toggleDocumentEnabled, getDocumentVersions, rollbackDocumentVersion, getVersionContent, getDocumentContent } from '@/api/knowledge'
import { getCategoryList } from '@/api/kbCategory'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadCategory = ref('')
const uploadFile = ref(null)
const uploadRef = ref()
const editUploadRef = ref()
const editFile = ref(null)
const categoryList = ref([])
const versionVisible = ref(false)
const versionLoading = ref(false)
const versionList = ref([])
const versionDocId = ref(null)
const previewVisible = ref(false)
const previewTitle = ref('')
const previewContent = ref('')

const searchForm = reactive({
  documentName: '',
  category: '',
  parseStatus: ''
})

const form = reactive({
  id: null,
  documentName: '',
  category: '',
  sourceType: 'MANUAL'
})

const rules = {
  documentName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }]
}

const statusTagType = (status) => {
  const map = { PENDING: 'info', PROCESSING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

const sourceLabel = (type) => {
  const map = { MANUAL: '手动上传', IMPORT: '系统导入', CRAWL: '爬虫采集' }
  return map[type] || type
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      documentName: searchForm.documentName || undefined,
      category: searchForm.category || undefined,
      parseStatus: searchForm.parseStatus || undefined
    }
    const res = await getDocumentPage(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.documentName = ''
  searchForm.category = ''
  searchForm.parseStatus = ''
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增文档'
  Object.assign(form, { id: null, documentName: '', category: '', sourceType: 'MANUAL' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑文档'
  Object.assign(form, {
    id: row.id,
    documentName: row.documentName,
    category: row.category,
    sourceType: row.sourceType
  })
  editFile.value = null
  dialogVisible.value = true
}

const handleEditFileChange = (file) => {
  editFile.value = file.raw
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该文档吗？删除后关联切片也将被清除。', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        if (editFile.value) {
          const fd = new FormData()
          fd.append('file', editFile.value)
          if (form.category) fd.append('category', form.category)
          await reuploadDocument(form.id, fd)
          ElMessage.success('文件已重新上传，正在解析中...')
        } else {
          await updateDocument(form)
          ElMessage.success('更新成功')
        }
      } else {
        await createDocument(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    }
  })
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    if (uploadCategory.value) {
      formData.append('category', uploadCategory.value)
    }
    const username = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
    if (username) {
      formData.append('uploadedBy', username)
    }
    await uploadDocument(formData)
    ElMessage.success('文档上传成功，正在后台解析和向量化')
    uploadVisible.value = false
    uploadFile.value = null
    uploadCategory.value = ''
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    uploading.value = false
  }
}

watch([pageNum, pageSize], () => {
  fetchData()
})

const handleToggleEnabled = async (row, val) => {
  try {
    await toggleDocumentEnabled(row.id, val)
    ElMessage.success(val ? '已启用' : '已禁用')
    fetchData()
  } catch (e) { console.error(e) }
}

const handleVersions = async (row) => {
  versionDocId.value = row.id
  versionVisible.value = true
  versionLoading.value = true
  try {
    const res = await getDocumentVersions(row.id)
    versionList.value = res.data || []
  } catch (e) { console.error(e) } finally { versionLoading.value = false }
}

const handlePreviewVersion = async (row) => {
  try {
    const res = await getVersionContent(versionDocId.value, row.version)
    const data = res.data || {}
    previewTitle.value = `${data.documentName || '文档'} (版本 ${row.version})`
    previewContent.value = data.content || '(无内容)'
    previewVisible.value = true
  } catch (e) { console.error(e) }
}

const handleDownloadVersion = async (row) => {
  try {
    const res = await getVersionContent(versionDocId.value, row.version)
    const data = res.data || {}
    const content = data.content || ''
    const fileName = data.documentName || row.documentName || 'document.txt'
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) { console.error(e) }
}

const handleRollback = async (row) => {
  try {
    await ElMessageBox.confirm(`确定回滚到版本 ${row.version}？`, '提示', { type: 'warning' })
    await rollbackDocumentVersion(versionDocId.value, row.version)
    ElMessage.success('回滚成功')
    versionVisible.value = false
    fetchData()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categoryList.value = res.data || []
  } catch (e) { console.error(e) }
}

onMounted(() => {
  fetchData()
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.knowledge-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
