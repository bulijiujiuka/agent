<template>
  <div class="ticket-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工单辅助</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增工单</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入工单标题" clearable />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="请选择" clearable style="width: 120px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.ticketStatus" placeholder="请选择" clearable style="width: 120px">
            <el-option label="待处理" value="OPEN" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="ticketNo" label="工单编号" width="180" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" width="110" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ticketStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ticketStatusTagType(row.ticketStatus)" size="small">{{ ticketStatusLabel(row.ticketStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="处理人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <div style="white-space: nowrap;">
              <el-button type="primary" link :icon="View" @click="handleDetail(row)">详情</el-button>
              <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工单标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="工单内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请描述问题" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="问题分类">
              <el-input v-model="form.category" placeholder="请输入分类" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option label="低" value="LOW" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="高" value="HIGH" />
                <el-option label="紧急" value="URGENT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="处理人">
              <el-input v-model="form.assignee" placeholder="处理人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.ticketStatus" style="width: 100%">
                <el-option label="待处理" value="OPEN" />
                <el-option label="处理中" value="IN_PROGRESS" />
                <el-option label="已解决" value="RESOLVED" />
                <el-option label="已关闭" value="CLOSED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="工单详情" width="800px" top="5vh">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; padding-right: 24px;">
          <span style="font-size: 16px; font-weight: 600;">工单详情</span>
          <div>
            <el-button size="small" :icon="Refresh" @click="handleRefreshDetail">刷新</el-button>
            <el-button size="small" type="warning" :loading="aiGenerating" @click="handleRegenerateAi">重新生成 AI 辅助</el-button>
          </div>
        </div>
      </template>
      <div class="detail-body">
        <div class="detail-section">
          <div class="detail-title">基本信息</div>
          <el-row :gutter="16" class="detail-grid">
            <el-col :span="12"><div class="detail-item"><span class="detail-label">工单编号</span><span class="detail-value">{{ detailRow.ticketNo }}</span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">标题</span><span class="detail-value">{{ detailRow.title }}</span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">客户</span><span class="detail-value">{{ detailRow.customerName || '-' }}</span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">分类</span><span class="detail-value">{{ detailRow.category || '-' }}</span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">优先级</span><span class="detail-value"><el-tag :type="priorityTagType(detailRow.priority)" size="small">{{ priorityLabel(detailRow.priority) }}</el-tag></span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">状态</span><span class="detail-value"><el-tag :type="ticketStatusTagType(detailRow.ticketStatus)" size="small">{{ ticketStatusLabel(detailRow.ticketStatus) }}</el-tag></span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">处理人</span><span class="detail-value">{{ detailRow.assignee || '-' }}</span></div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">创建时间</span><span class="detail-value">{{ detailRow.createTime }}</span></div></el-col>
          </el-row>
        </div>
        <div class="detail-section">
          <div class="detail-title">工单内容</div>
          <div class="detail-content-box">{{ detailRow.content || '-' }}</div>
        </div>
        <div class="detail-section ai-section">
          <div class="detail-title">AI 辅助分析</div>
          <div class="ai-card">
            <div class="ai-card-title">📋 问题摘要</div>
            <div class="ai-card-body" v-if="detailRow.aiSummary">{{ detailRow.aiSummary }}</div>
            <el-tag v-else type="info" size="small">等待生成…</el-tag>
          </div>
          <div class="ai-card">
            <div class="ai-card-title">💬 回复建议</div>
            <div class="ai-card-body" v-if="detailRow.aiReply">{{ detailRow.aiReply }}</div>
            <el-tag v-else type="info" size="small">等待生成…</el-tag>
          </div>
          <div class="ai-card">
            <div class="ai-card-title">🔧 处理建议</div>
            <div class="ai-card-body" v-if="detailRow.aiSuggestion">{{ detailRow.aiSuggestion }}</div>
            <el-tag v-else type="info" size="small">等待生成…</el-tag>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus, Search, Refresh, Edit, Delete, View } from '@element-plus/icons-vue'
import { getTicketPage, getTicketById, createTicket, updateTicket, deleteTicket, regenerateAiAssist } from '@/api/ticket'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const detailVisible = ref(false)
const detailRow = ref({})
const aiGenerating = ref(false)

const searchForm = reactive({
  title: '',
  priority: '',
  ticketStatus: ''
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  customerName: '',
  category: '',
  priority: 'MEDIUM',
  ticketStatus: 'OPEN',
  assignee: ''
})

const rules = {
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入工单内容', trigger: 'blur' }]
}

const priorityTagType = (p) => {
  const map = { LOW: 'info', MEDIUM: '', HIGH: 'warning', URGENT: 'danger' }
  return map[p] || ''
}
const priorityLabel = (p) => {
  const map = { LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急' }
  return map[p] || p
}
const ticketStatusTagType = (s) => {
  const map = { OPEN: 'info', IN_PROGRESS: 'warning', RESOLVED: 'success', CLOSED: '' }
  return map[s] || ''
}
const ticketStatusLabel = (s) => {
  const map = { OPEN: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[s] || s
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      title: searchForm.title || undefined,
      priority: searchForm.priority || undefined,
      ticketStatus: searchForm.ticketStatus || undefined
    }
    const res = await getTicketPage(params)
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
  searchForm.title = ''
  searchForm.priority = ''
  searchForm.ticketStatus = ''
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增工单'
  Object.assign(form, { id: null, title: '', content: '', customerName: '', category: '', priority: 'MEDIUM', ticketStatus: 'OPEN', assignee: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑工单'
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content,
    customerName: row.customerName,
    category: row.category,
    priority: row.priority,
    ticketStatus: row.ticketStatus,
    assignee: row.assignee
  })
  dialogVisible.value = true
}

const handleDetail = async (row) => {
  try {
    const res = await getTicketById(row.id)
    detailRow.value = res.data || row
  } catch (e) {
    detailRow.value = row
  }
  detailVisible.value = true
}

const handleRefreshDetail = async () => {
  if (!detailRow.value?.id) return
  try {
    const res = await getTicketById(detailRow.value.id)
    detailRow.value = res.data || detailRow.value
    ElMessage.success('已刷新')
  } catch (e) {
    console.error(e)
  }
}

const handleRegenerateAi = async () => {
  if (!detailRow.value?.id) return
  aiGenerating.value = true
  try {
    await regenerateAiAssist(detailRow.value.id)
    ElMessage.success('AI 辅助正在后台重新生成，请稍后点击刷新查看')
  } catch (e) {
    console.error(e)
  } finally {
    aiGenerating.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该工单吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteTicket(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        await updateTicket(form)
        ElMessage.success('更新成功')
      } else {
        await createTicket(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    }
  })
}

watch([pageNum, pageSize], () => {
  fetchData()
})

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.ticket-container {
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

.detail-body {
  max-height: 70vh;
  overflow-y: auto;

  .detail-section {
    margin-bottom: 20px;

    .detail-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      padding-left: 10px;
      border-left: 3px solid #409eff;
    }
  }

  .detail-grid {
    .el-col {
      margin-bottom: 12px;
    }

    .detail-item {
      display: flex;
      align-items: center;
      font-size: 14px;

      .detail-label {
        color: #909399;
        min-width: 70px;
        margin-right: 12px;
        white-space: nowrap;
      }

      .detail-value {
        color: #303133;
        flex: 1;
        word-break: break-all;
      }
    }
  }

  .detail-content-box {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 14px 16px;
    font-size: 14px;
    color: #303133;
    line-height: 1.7;
    white-space: pre-wrap;
  }

  .ai-section {
    .detail-title {
      border-left-color: #e6a23c;
    }

    .ai-card {
      background: #fdf6ec;
      border: 1px solid #faecd8;
      border-radius: 8px;
      padding: 14px 16px;
      margin-bottom: 12px;

      .ai-card-title {
        font-size: 13px;
        font-weight: 600;
        color: #e6a23c;
        margin-bottom: 8px;
      }

      .ai-card-body {
        font-size: 14px;
        color: #303133;
        line-height: 1.7;
        white-space: pre-wrap;
      }
    }
  }
}
</style>
