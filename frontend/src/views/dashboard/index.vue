<template>
  <div class="dashboard-container">
    <div class="welcome-bar">
      <div class="welcome-left">
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ userStore.userInfo?.nickname || 'Admin' }}</h2>
          <p>{{ currentDate }}</p>
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div v-for="item in summaryCards" :key="item.title" class="stat-card">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.title }}</div>
      </div>
    </div>

    <div class="main-row">
      <div class="section-card">
        <div class="section-header">快速入口</div>
        <div class="quick-list">
          <div class="quick-item" @click="router.push('/knowledge')">
            <span class="quick-name">知识库管理</span>
            <span class="quick-desc">上传文档、切分与向量化</span>
            <span class="quick-arrow">›</span>
          </div>
          <div class="quick-item" @click="router.push('/ai/chat')">
            <span class="quick-name">AI 智能问答</span>
            <span class="quick-desc">基于知识库的检索增强对话</span>
            <span class="quick-arrow">›</span>
          </div>
          <div class="quick-item" @click="router.push('/ticket')">
            <span class="quick-name">工单辅助</span>
            <span class="quick-desc">AI 自动生成摘要与处理建议</span>
            <span class="quick-arrow">›</span>
          </div>
          <div class="quick-item" @click="router.push('/system/user')">
            <span class="quick-name">系统设置</span>
            <span class="quick-desc">用户、角色与操作日志</span>
            <span class="quick-arrow">›</span>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-header">系统能力</div>
        <div class="feature-list">
          <div class="feature-item" v-for="f in features" :key="f.title">
            <span class="feature-name">{{ f.title }}</span>
            <span class="feature-desc">{{ f.desc }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/dashboard'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({ documentCount: 0, ticketCount: 0, conversationCount: 0 })

const summaryCards = computed(() => [
  { title: '知识文档', value: stats.value.documentCount },
  { title: 'AI 会话', value: stats.value.conversationCount },
  { title: '工单总数', value: stats.value.ticketCount }
])

const features = [
  { title: '知识库 RAG 检索', desc: '文档上传 → 切分 → Embedding → 语义检索' },
  { title: 'AI 流式问答', desc: 'DeepSeek 大模型 + SSE 实时生成' },
  { title: '工单 AI 辅助', desc: '自动生成摘要、回复建议、处理步骤' },
  { title: 'Markdown 渲染', desc: '标题 / 列表 / 代码块 / 表格格式化' },
  { title: '多轮对话', desc: '上下文记忆的连续智能对话' },
  { title: '启动自动索引', desc: '重启后自动从 DB 重建向量库' }
]

const currentDate = computed(() => {
  const d = new Date()
  const weekMap = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekMap[d.getDay()]}`
})

const fetchStats = async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || stats.value
  } catch (e) {
    console.error(e)
  }
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

onMounted(() => {
  fetchStats()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.welcome-bar {
  .welcome-text {
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 4px;
    }
    p {
      font-size: 13px;
      color: #909399;
    }
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .stat-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 20px 24px;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 8px;
    }
  }
}

.main-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.section-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px 24px;

  .section-header {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 14px;
  }
}

.quick-list {
  display: flex;
  flex-direction: column;

  .quick-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    transition: background 0.15s;

    &:last-child { border-bottom: none; }
    &:hover { background: #fafafa; margin: 0 -24px; padding: 12px 24px; }

    .quick-name {
      font-size: 14px;
      color: #303133;
      font-weight: 500;
      min-width: 100px;
    }

    .quick-desc {
      flex: 1;
      font-size: 13px;
      color: #909399;
      margin-left: 12px;
    }

    .quick-arrow {
      font-size: 18px;
      color: #c0c4cc;
      margin-left: 8px;
    }
  }
}

.feature-list {
  display: flex;
  flex-direction: column;

  .feature-item {
    display: flex;
    align-items: baseline;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child { border-bottom: none; }

    .feature-name {
      font-size: 14px;
      color: #303133;
      font-weight: 500;
      min-width: 120px;
    }

    .feature-desc {
      font-size: 13px;
      color: #909399;
      margin-left: 12px;
    }
  }
}
</style>
