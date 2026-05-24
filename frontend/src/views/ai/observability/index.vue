<template>
  <div class="observability-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="调用链日志" name="logs">
        <el-card>
          <el-form :inline="true" style="margin-bottom:12px">
            <el-form-item label="步骤">
              <el-select v-model="logQuery.stepName" placeholder="全部" clearable style="width:150px">
                <el-option label="Embedding" value="EMBEDDING" />
                <el-option label="检索" value="RETRIEVAL" />
                <el-option label="模型调用" value="LLM_CALL" />
                <el-option label="过滤" value="FILTER" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="logQuery.success" placeholder="全部" clearable style="width:110px">
                <el-option label="成功" :value="true" />
                <el-option label="失败" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchLogs">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="logs" v-loading="logsLoading" stripe>
            <el-table-column prop="traceId" label="TraceID" width="160" show-overflow-tooltip />
            <el-table-column prop="conversationId" label="会话ID" width="80" align="center" />
            <el-table-column prop="stepName" label="步骤" width="110">
              <template #default="{ row }">
                <el-tag :type="stepTagType(row.stepName)" size="small">{{ stepLabel(row.stepName) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="modelKey" label="模型" width="110" />
            <el-table-column prop="inputLength" label="输入字符" width="90" align="center" />
            <el-table-column prop="outputLength" label="输出字符" width="90" align="center" />
            <el-table-column prop="durationMs" label="耗时(ms)" width="90" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.durationMs > 5000 ? '#f56c6c' : row.durationMs > 2000 ? '#e6a23c' : '#67c23a' }">
                  {{ row.durationMs }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="success" label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
          <div class="pagination-wrap">
            <el-pagination :current-page="logQuery.page" :page-size="logQuery.size" :total="logsTotal"
              layout="total, prev, pager, next" @current-change="(val) => { logQuery.page = val; fetchLogs() }" />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="步骤统计" name="stats">
        <el-card>
          <el-table :data="stepStats" v-loading="statsLoading" stripe>
            <el-table-column prop="stepName" label="步骤" width="150">
              <template #default="{ row }">
                <el-tag :type="stepTagType(row.stepName)" size="small">{{ stepLabel(row.stepName) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cnt" label="调用次数" width="120" align="center" />
            <el-table-column label="平均耗时(ms)" width="140" align="center">
              <template #default="{ row }">{{ Math.round(row.avgMs) }}</template>
            </el-table-column>
            <el-table-column prop="failCount" label="失败次数" width="120" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.failCount > 0 ? '#f56c6c' : '#67c23a' }">{{ row.failCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成功率" width="120" align="center">
              <template #default="{ row }">
                {{ row.cnt > 0 ? ((1 - row.failCount / row.cnt) * 100).toFixed(1) + '%' : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="检索评测" name="eval">
        <el-card>
          <div class="eval-grid" v-loading="evalLoading">
            <div class="eval-card" v-for="item in evalCards" :key="item.label">
              <div class="eval-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="eval-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCallLogs, getCallLogStats, getRetrievalEval } from '@/api/aiAudit'

const activeTab = ref('logs')

const logsLoading = ref(false)
const logs = ref([])
const logsTotal = ref(0)
const logQuery = ref({ stepName: '', success: null, page: 1, size: 20 })

const statsLoading = ref(false)
const stepStats = ref([])

const evalLoading = ref(false)
const evalCards = ref([])

const stepLabelMap = { EMBEDDING: '向量化', RETRIEVAL: '检索', LLM_CALL: '模型调用', FILTER: '过滤' }
const stepLabel = (name) => stepLabelMap[name] || name
const stepTagType = (name) => {
  const map = { EMBEDDING: '', RETRIEVAL: 'warning', LLM_CALL: 'success', FILTER: 'info' }
  return map[name] || ''
}

const fetchLogs = async () => {
  logsLoading.value = true
  try {
    const params = { page: logQuery.value.page, size: logQuery.value.size }
    if (logQuery.value.stepName) params.stepName = logQuery.value.stepName
    if (logQuery.value.success !== null && logQuery.value.success !== '') params.success = logQuery.value.success
    const res = await getCallLogs(params)
    logs.value = res.data?.records || []
    logsTotal.value = res.data?.total || 0
  } catch (e) { console.error(e) } finally { logsLoading.value = false }
}

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await getCallLogStats(7)
    stepStats.value = res.data || []
  } catch (e) { console.error(e) } finally { statsLoading.value = false }
}

const fetchEval = async () => {
  evalLoading.value = true
  try {
    const res = await getRetrievalEval()
    const d = res.data || {}
    evalCards.value = [
      { label: '检索命中率', value: d.hitRate + '%', color: '#409eff' },
      { label: '平均检索分数', value: (d.avgRetrievalScore || 0).toFixed(3), color: '#67c23a' },
      { label: '平均命中条数', value: (d.avgRetrievalHitCount || 0).toFixed(1), color: '#e6a23c' },
      { label: '命中回答数', value: d.countWithHits, color: '#409eff' },
      { label: '未命中回答数', value: d.countWithoutHits, color: '#f56c6c' },
      { label: '用户满意率', value: d.satisfactionRate + '%', color: '#67c23a' },
      { label: '平均响应耗时', value: d.avgResponseMs + 'ms', color: '#e6a23c' },
      { label: '好评数', value: d.likeCount, color: '#67c23a' },
      { label: '差评数', value: d.dislikeCount, color: '#f56c6c' }
    ]
  } catch (e) { console.error(e) } finally { evalLoading.value = false }
}

onMounted(() => { fetchLogs(); fetchStats(); fetchEval() })
</script>

<style lang="scss" scoped>
.observability-container {
  .pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
}

.eval-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .eval-card {
    background: #f5f7fa;
    border-radius: 8px;
    padding: 24px;
    text-align: center;
    .eval-value { font-size: 28px; font-weight: 700; }
    .eval-label { font-size: 13px; color: #909399; margin-top: 8px; }
  }
}
</style>
