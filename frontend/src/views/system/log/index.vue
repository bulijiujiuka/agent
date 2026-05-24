<template>
  <div class="log-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <div>
            <el-button type="danger" :icon="Delete" @click="handleClear">清空日志</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作模块">
          <el-input v-model="searchForm.module" placeholder="请输入模块" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="操作用户">
          <el-input v-model="searchForm.operUser" placeholder="请输入用户" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="description" label="操作" width="120" />
        <el-table-column prop="requestMethod" label="方法" width="80">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operUser" label="操作用户" width="100" />
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column prop="costTime" label="耗时" width="80">
          <template #default="{ row }">{{ row.costTime }}ms</template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模块">{{ detailData.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detailData.description }}</el-descriptions-item>
        <el-descriptions-item label="方法" :span="2">{{ detailData.method }}</el-descriptions-item>
        <el-descriptions-item label="URL" :span="2">{{ detailData.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div class="json-text">{{ detailData.requestParams }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <div class="json-text">{{ detailData.responseResult }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.status === 1 ? 'success' : 'danger'" size="small">
            {{ detailData.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ detailData.operUser }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detailData.operIp }}</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.errorMsg" label="错误信息" :span="2">
          <span style="color: #f56c6c;">{{ detailData.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOperLogPage, deleteOperLog, clearOperLog } from '@/api/operLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailData = ref({})

const searchForm = reactive({ module: '', operUser: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      module: searchForm.module || undefined,
      operUser: searchForm.operUser || undefined
    }
    const res = await getOperLogPage(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pageNum.value = 1; fetchData() }
const handleReset = () => { searchForm.module = ''; searchForm.operUser = ''; pageNum.value = 1; fetchData() }

const handleDetail = (row) => { detailData.value = row; detailVisible.value = true }

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该日志？', '提示', { type: 'warning' }).then(async () => {
    await deleteOperLog(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleClear = () => {
  ElMessageBox.confirm('确定清空所有操作日志？此操作不可恢复！', '警告', { type: 'error' }).then(async () => {
    await clearOperLog()
    ElMessage.success('已清空')
    fetchData()
  })
}

const methodTagType = (method) => {
  const map = { GET: 'info', POST: 'success', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

watch([pageNum, pageSize], () => fetchData())
onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
.log-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .search-form {
    margin-bottom: 16px;
  }
  .json-text {
    max-height: 200px;
    overflow-y: auto;
    word-break: break-all;
    white-space: pre-wrap;
    font-size: 12px;
    color: #606266;
  }
}
</style>
