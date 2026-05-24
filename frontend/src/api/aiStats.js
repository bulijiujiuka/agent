import request from './request'

export function getStatsOverview() {
  return request({ url: '/ai/stats/overview', method: 'get' })
}

export function getStatsTrend(days = 14) {
  return request({ url: '/ai/stats/trend', method: 'get', params: { days } })
}
