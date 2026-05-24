<template>
  <div class="stats-container">
    <div class="overview-row">
      <div v-for="item in overviewCards" :key="item.label" class="overview-card">
        <div class="ov-value">{{ item.value }}</div>
        <div class="ov-label">{{ item.label }}</div>
      </div>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>近 14 天 AI 回复数趋势</span></template>
        <div ref="msgChartRef" class="chart-box"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>近 14 天会话数趋势</span></template>
        <div ref="convChartRef" class="chart-box"></div>
      </el-card>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>用户反馈分布</span></template>
        <div ref="feedbackChartRef" class="chart-box"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>核心指标</span></template>
        <div class="metric-list">
          <div class="metric-item">
            <span class="metric-label">平均响应耗时</span>
            <span class="metric-value">{{ overview.avgResponseMs }} ms</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">累计 Token 消耗</span>
            <span class="metric-value">{{ overview.totalTokenUsage }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">AI 回复总数</span>
            <span class="metric-value">{{ overview.totalReplies }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">知识文档数</span>
            <span class="metric-value">{{ overview.documentCount }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">工单总数</span>
            <span class="metric-value">{{ overview.ticketCount }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getStatsTrend } from '@/api/aiStats'

const msgChartRef = ref()
const convChartRef = ref()
const feedbackChartRef = ref()

let msgChart = null
let convChart = null
let feedbackChart = null

const overview = ref({
  likeCount: 0, dislikeCount: 0, totalReplies: 0,
  avgResponseMs: 0, totalTokenUsage: 0, conversationCount: 0,
  documentCount: 0, ticketCount: 0
})

const overviewCards = ref([])

const buildOverviewCards = () => {
  const o = overview.value
  overviewCards.value = [
    { label: 'AI 会话', value: o.conversationCount },
    { label: 'AI 回复', value: o.totalReplies },
    { label: '点赞', value: o.likeCount },
    { label: '点踩', value: o.dislikeCount }
  ]
}

const fetchOverview = async () => {
  try {
    const res = await getStatsOverview()
    overview.value = res.data || overview.value
    buildOverviewCards()
    renderFeedbackChart()
  } catch (e) { console.error(e) }
}

const fetchTrend = async () => {
  try {
    const res = await getStatsTrend(14)
    const data = res.data || {}
    renderLineChart(msgChart, msgChartRef.value, data.dailyMessages || [], '#409eff')
    renderLineChart(convChart, convChartRef.value, data.dailyConversations || [], '#67c23a')
  } catch (e) { console.error(e) }
}

const renderLineChart = (chartInstance, dom, list, color) => {
  if (!dom) return
  if (chartInstance) chartInstance.dispose()
  const chart = echarts.init(dom)
  const days = list.map(i => i.day)
  const values = list.map(i => i.cnt)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: days, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', data: values, smooth: true, itemStyle: { color }, areaStyle: { color: color + '20' } }]
  })
  return chart
}

const renderFeedbackChart = () => {
  if (!feedbackChartRef.value) return
  if (feedbackChart) feedbackChart.dispose()
  feedbackChart = echarts.init(feedbackChartRef.value)
  const o = overview.value
  feedbackChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: [
        { value: o.likeCount, name: '点赞', itemStyle: { color: '#67c23a' } },
        { value: o.dislikeCount, name: '点踩', itemStyle: { color: '#f56c6c' } },
        { value: Math.max(0, o.totalReplies - o.likeCount - o.dislikeCount), name: '未评价', itemStyle: { color: '#dcdfe6' } }
      ],
      label: { formatter: '{b}: {c}' }
    }]
  })
}

const handleResize = () => {
  msgChart && msgChart.resize()
  convChart && convChart.resize()
  feedbackChart && feedbackChart.resize()
}

onMounted(async () => {
  await fetchOverview()
  await nextTick()
  await fetchTrend()
  msgChart = msgChartRef.value ? echarts.getInstanceByDom(msgChartRef.value) : null
  convChart = convChartRef.value ? echarts.getInstanceByDom(convChartRef.value) : null
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  msgChart && msgChart.dispose()
  convChart && convChart.dispose()
  feedbackChart && feedbackChart.dispose()
})
</script>

<style lang="scss" scoped>
.stats-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  .overview-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 20px 24px;

    .ov-value { font-size: 28px; font-weight: 700; color: #303133; }
    .ov-label { font-size: 13px; color: #909399; margin-top: 6px; }
  }
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  .chart-box { width: 100%; height: 280px; }
}

.metric-list {
  .metric-item {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
    &:last-child { border-bottom: none; }

    .metric-label { font-size: 14px; color: #606266; }
    .metric-value { font-size: 14px; font-weight: 600; color: #303133; }
  }
}
</style>
