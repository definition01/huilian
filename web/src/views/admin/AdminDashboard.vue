<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2>数据概览</h2>
      <n-button size="small" :loading="loading" @click="loadStats">刷新</n-button>
    </div>

    <n-spin :show="loading">
      <div class="stat-grid">
        <div v-for="card in cards" :key="card.key" class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
            <component :is="card.icon" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats[card.key] ?? 0 }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-card">
          <div class="detail-title">用户活跃情况</div>
          <div class="detail-row">
            <span>启用用户</span>
            <strong>{{ stats.activeUsers ?? 0 }}</strong>
          </div>
          <div class="detail-row">
            <span>禁用用户</span>
            <strong>{{ stats.disabledUsers ?? 0 }}</strong>
          </div>
        </div>
        <div class="detail-card">
          <div class="detail-title">会议情况</div>
          <div class="detail-row">
            <span>会议总数</span>
            <strong>{{ stats.totalMeetings ?? 0 }}</strong>
          </div>
          <div class="detail-row">
            <span>进行中会议</span>
            <strong>{{ stats.ongoingMeetings ?? 0 }}</strong>
          </div>
        </div>
        <div class="detail-card">
          <div class="detail-title">群聊情况</div>
          <div class="detail-row">
            <span>群聊总数</span>
            <strong>{{ stats.totalGroups ?? 0 }}</strong>
          </div>
          <div class="detail-row">
            <span>群成员总数</span>
            <strong>{{ stats.totalGroupMembers ?? 0 }}</strong>
          </div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { NButton, NSpin, useMessage } from 'naive-ui'
import { getAdminStats } from '@/api/admin'

const message = useMessage()
const loading = ref(false)
const stats = ref({})

const iconSvg = path => () => h('svg', {
  width: 22,
  height: 22,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': 2
}, path.map(p => h('path', p)))

const cards = [
  {
    key: 'totalUsers',
    label: '用户总数',
    bg: '#eaf2ff',
    color: '#006EFF',
    icon: iconSvg([{ d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }, { d: 'M9 7m-4 0a4 4 0 1 0 8 0 4 4 0 0 0-8 0' }])
  },
  {
    key: 'totalMeetings',
    label: '会议总数',
    bg: '#eafaf1',
    color: '#12b76a',
    icon: iconSvg([{ d: 'M3 4h18v18H3z' }, { d: 'M16 2v4M8 2v4M3 10h18' }])
  },
  {
    key: 'totalGroups',
    label: '群聊总数',
    bg: '#fef3e2',
    color: '#f79009',
    icon: iconSvg([{ d: 'M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z' }])
  },
  {
    key: 'ongoingMeetings',
    label: '进行中会议',
    bg: '#fdeef4',
    color: '#e84c88',
    icon: iconSvg([{ d: 'M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z' }])
  }
]

async function loadStats() {
  loading.value = true
  try {
    const res = await getAdminStats()
    stats.value = res.data || {}
  } catch (e) {
    message.error(e.message || '获取统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    color: #1f2937;
    font-weight: 600;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.detail-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
}

.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-top: 1px solid #f3f4f6;
  font-size: 14px;
  color: #6b7280;

  strong {
    color: #1f2937;
    font-weight: 600;
  }
}
</style>
