<template>
  <div class="audit-container">
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="按用户ID筛选" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchConversations">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:12px">
      <template #header><span>会话审计</span></template>
      <el-table :data="conversations" v-loading="loading" stripe @row-click="handleRowClick" style="cursor:pointer">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="conversationNo" label="会话编号" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="modelName" label="模型" width="120" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination :current-page="query.page" :page-size="query.size" :total="total"
          layout="total, prev, pager, next" @current-change="(val) => { query.page = val; fetchConversations() }" />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="对话详情" size="55%">
      <div class="message-list" v-if="messages.length">
        <div v-for="msg in messages" :key="msg.id" :class="['msg-item', msg.role === 'USER' ? 'msg-user' : 'msg-assistant']">
          <div class="msg-role">{{ msg.role === 'USER' ? '👤 用户' : '🤖 AI' }}</div>
          <div class="msg-content">{{ msg.content }}</div>
          <div class="msg-meta">
            <span v-if="msg.responseMs != null">耗时 {{ msg.responseMs }}ms</span>
            <span v-if="msg.tokenUsage != null"> · {{ msg.tokenUsage }} tokens</span>
            <span v-if="msg.retrievalScore != null"> · 检索分 {{ msg.retrievalScore.toFixed(3) }}</span>
            <span v-if="msg.retrievalHitCount != null"> · 命中 {{ msg.retrievalHitCount }} 条</span>
            <span v-if="msg.feedback"> · 反馈: {{ msg.feedback }}</span>
            <span class="msg-time"> · {{ msg.createTime }}</span>
          </div>
          <div v-if="msg.referenceContent" class="msg-ref">
            <el-collapse>
              <el-collapse-item title="引用来源">
                <pre class="ref-text">{{ msg.referenceContent }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无消息" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditConversations, getAuditMessages } from '@/api/aiAudit'

const loading = ref(false)
const conversations = ref([])
const total = ref(0)
const query = ref({ userId: '', status: '', page: 1, size: 20 })
const drawerVisible = ref(false)
const messages = ref([])

const fetchConversations = async () => {
  loading.value = true
  try {
    const params = { page: query.value.page, size: query.value.size }
    if (query.value.userId) params.userId = query.value.userId
    if (query.value.status) params.status = query.value.status
    const res = await getAuditConversations(params)
    conversations.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleRowClick = async (row) => {
  drawerVisible.value = true
  try {
    const res = await getAuditMessages(row.id)
    messages.value = res.data || []
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchConversations() })
</script>

<style lang="scss" scoped>
.audit-container {
  .filter-card { :deep(.el-card__body) { padding-bottom: 0; } }
  .pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
}

.message-list {
  .msg-item {
    padding: 12px 16px;
    margin-bottom: 12px;
    border-radius: 8px;
    &.msg-user { background: #ecf5ff; }
    &.msg-assistant { background: #f5f7fa; }
    .msg-role { font-weight: 600; font-size: 13px; margin-bottom: 6px; }
    .msg-content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
    .msg-meta { font-size: 12px; color: #909399; margin-top: 8px; }
    .msg-ref { margin-top: 8px; }
    .ref-text { font-size: 12px; color: #606266; white-space: pre-wrap; max-height: 200px; overflow-y: auto; }
  }
}
</style>
