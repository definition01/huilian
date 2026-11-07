<template>
  <div class="meeting-list-page">
    <div class="page-header">
      <h1>会议列表</h1>
      <div class="header-actions">
        <n-input v-model:value="searchKeyword" placeholder="搜索会议" style="width: 200px" clearable />
        <n-select v-model:value="statusFilter" :options="statusOptions" placeholder="状态筛选" style="width: 140px" clearable />
      </div>
    </div>

    <div class="meeting-table">
      <n-data-table
        :columns="columns"
        :data="filteredMeetings"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        remote
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <!-- 预约会议弹窗 -->
    <n-modal v-model:show="showScheduleModal" preset="dialog" title="预约会议" positive-text="预约" negative-text="取消" :loading="scheduling" @positive-click="handleSchedule">
      <n-form :model="scheduleForm" label-placement="left" label-width="80">
        <n-form-item label="会议主题">
          <n-input v-model:value="scheduleForm.topic" placeholder="请输入会议主题" />
        </n-form-item>
        <n-form-item label="开始时间">
          <n-date-picker v-model:value="scheduleForm.scheduledStartTime" type="datetime" style="width: 100%" />
        </n-form-item>
        <n-form-item label="持续时长">
          <n-input-number v-model:value="scheduleForm.duration" :min="15" :max="480" :step="15" style="width: 100%">
            <template #suffix>分钟</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="入会密码">
          <n-input v-model:value="scheduleForm.password" placeholder="留空则无密码" />
        </n-form-item>
      </n-form>
    </n-modal>
    <!-- AI 会议纪要弹窗 -->
    <n-modal v-model:show="showSummaryModal" preset="card" title="AI 会议纪要" style="width: 680px; max-width: 92vw">
      <div v-if="summaryLoading" class="summary-loading">
        <n-spin size="medium" />
      </div>
      <template v-else-if="summary">
        <div class="summary-head">
          <span class="summary-subject">{{ summary.title || '会议纪要' }}</span>
          <n-tag size="small" :type="summaryTagType">{{ summaryTagText }}</n-tag>
        </div>

        <template v-if="summary.status === 'success'">
          <p class="summary-overview">{{ summary.overview }}</p>

          <div v-if="summary.keyPoints?.length" class="summary-block">
            <div class="summary-block-title">关键要点</div>
            <ul>
              <li v-for="(point, idx) in summary.keyPoints" :key="'kp-' + idx">{{ point }}</li>
            </ul>
          </div>

          <div v-if="summary.decisions?.length" class="summary-block">
            <div class="summary-block-title">决议事项</div>
            <ul>
              <li v-for="(item, idx) in summary.decisions" :key="'dc-' + idx">{{ item }}</li>
            </ul>
          </div>

          <div v-if="summary.actionItems?.length" class="summary-block">
            <div class="summary-block-title">待办事项</div>
            <ul>
              <li v-for="(item, idx) in summary.actionItems" :key="'ac-' + idx">
                {{ item.task }}
                <span v-if="item.owner" class="action-owner">@{{ item.owner }}</span>
                <span v-if="item.dueDate" class="action-due">{{ item.dueDate }}</span>
              </li>
            </ul>
          </div>

          <div v-if="summary.keywords?.length" class="summary-keywords">
            <n-tag v-for="(kw, idx) in summary.keywords" :key="'kw-' + idx" size="small" round :bordered="false">
              {{ kw }}
            </n-tag>
          </div>
        </template>

        <n-empty
          v-else
          :description="summary.status === 'failed'
            ? (summary.errorMessage || 'AI 纪要生成失败')
            : 'AI 正在整理会议内容，请稍后刷新'"
        />
      </template>
      <n-empty v-else description="暂无会议纪要（需开启 AI 总结且会议有有效发言）" />

      <template #footer>
        <div class="summary-footer">
          <n-button size="small" :loading="summaryLoading" @click="loadSummaryDetail">刷新</n-button>
          <n-button size="small" @click="showSummaryModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, h } from 'vue'
import { useRouter } from 'vue-router'
import { NDataTable, NButton, NTag, NInput, NSelect, NModal, NForm, NFormItem, NInputNumber, NDatePicker, NSpin, NEmpty, useMessage } from 'naive-ui'
import { useMeetingStore } from '@/store/meeting'
import { useUserStore } from '@/store/user'
import { getMeetingSummary } from '@/api/ai'
import dayjs from 'dayjs'

const router = useRouter()
const meetingStore = useMeetingStore()
const userStore = useUserStore()
const message = useMessage()

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref(null)
const showScheduleModal = ref(false)
const scheduling = ref(false)

// AI 会议纪要
const showSummaryModal = ref(false)
const summaryLoading = ref(false)
const summary = ref(null)
const summaryMeetingId = ref(null)
const summaryTagType = computed(() => {
  if (summary.value?.status === 'success') return 'success'
  if (summary.value?.status === 'failed') return 'error'
  return 'warning'
})
const summaryTagText = computed(() => {
  if (summary.value?.status === 'success') return '已生成'
  if (summary.value?.status === 'failed') return '生成失败'
  return '生成中'
})

const scheduleForm = ref({
  topic: '预约会议',
  scheduledStartTime: Date.now() + 3600000,
  duration: 60,
  password: ''
})

const statusOptions = [
  { label: '进行中', value: 'ongoing' },
  { label: '未开始', value: 'waiting' },
  { label: '已结束', value: 'ended' },
  { label: '已取消', value: 'cancelled' }
]

const columns = computed(() => [
  { title: '会议主题', key: 'topic', ellipsis: { tooltip: true } },
  { title: '会议号', key: 'meetingNumber', width: 130 },
  {
    title: '类型',
    key: 'type',
    width: 100,
    render(row) {
      return h(NTag, { type: row.type === 'instant' ? 'info' : 'warning', size: 'small' }, () => row.type === 'instant' ? '即时' : '预约')
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const typeMap = { ongoing: 'success', waiting: 'warning', ended: 'info', cancelled: 'error' }
      const textMap = { ongoing: '进行中', waiting: '未开始', ended: '已结束', cancelled: '已取消' }
      return h(NTag, { type: typeMap[row.status] || 'default', size: 'small' }, () => textMap[row.status] || row.status)
    }
  },
  { title: '参会人数', key: 'currentParticipants', width: 100, render(row) { return `${row.currentParticipants}/${row.maxParticipants}` } },
  { title: '创建时间', key: 'createdAt', width: 160, render(row) { return dayjs(row.createdAt).format('YYYY-MM-DD HH:mm') } },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render(row) {
      const buttons = []
      const isCreator = row.hostUserId != null && row.hostUserId === userStore.userInfo?.id
      if (row.status === 'ongoing' || row.status === 'waiting') {
        buttons.push(h(NButton, { size: 'small', type: 'primary', onClick: () => router.push(`/meeting/${row.id}`) }, () => '进入'))
      }
      // 只有创建者可以取消会议，非创建者不显示取消按钮
      if (isCreator && row.status !== 'ended' && row.status !== 'cancelled') {
        buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleCancel(row.id), style: 'margin-left: 8px' }, () => '取消'))
      }
      if (row.status === 'ended') {
        buttons.push(h(NButton, { size: 'small', type: 'info', ghost: true, onClick: () => openSummary(row), style: 'margin-left: 8px' }, () => 'AI 纪要'))
      }
      return h('div', {}, buttons)
    }
  }
])

const filteredMeetings = computed(() => {
  let list = meetingStore.meetingList
  if (searchKeyword.value) {
    list = list.filter(m => m.topic.includes(searchKeyword.value) || m.meetingNumber.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(m => m.status === statusFilter.value)
  }
  return list
})

const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: ({ itemCount }) => `共 ${itemCount} 条`
})

onMounted(() => {
  fetchList()
})

// 状态筛选走后端分页查询，避免只在当前页数据里过滤
watch(statusFilter, () => {
  pagination.value.page = 1
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const res = await meetingStore.fetchMyMeetings(
      pagination.value.page,
      pagination.value.pageSize,
      statusFilter.value || undefined
    )
    pagination.value.itemCount = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  pagination.value.page = page
  fetchList()
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  fetchList()
}

async function handleCancel(meetingId) {
  try {
    await meetingStore.cancelCurrentMeeting(meetingId)
    message.success('已取消')
    fetchList()
  } catch (e) {
    message.error(e?.message || '取消失败')
  }
}

async function openSummary(row) {
  summaryMeetingId.value = row.id
  summary.value = null
  showSummaryModal.value = true
  await loadSummaryDetail()
}

async function loadSummaryDetail() {
  if (!summaryMeetingId.value) return
  summaryLoading.value = true
  try {
    const res = await getMeetingSummary(summaryMeetingId.value)
    summary.value = res.data || null
  } catch (e) {
    message.error('获取会议纪要失败')
  } finally {
    summaryLoading.value = false
  }
}

// 后端要求密码必须为 6 位，为空时不能提交空字符串
function normalizePassword(data) {
  const payload = { ...data }
  const password = payload.password?.trim()
  if (password) {
    payload.password = password
  } else {
    delete payload.password
  }
  return payload
}

async function handleSchedule() {
  scheduling.value = true
  try {
    await meetingStore.createScheduled({
      ...normalizePassword(scheduleForm.value),
      duration: scheduleForm.value.duration || 60,
      scheduledStartTime: dayjs(scheduleForm.value.scheduledStartTime).format('YYYY-MM-DD HH:mm:ss')
    })
    message.success('预约成功')
    showScheduleModal.value = false
    fetchList()
  } catch (e) {
    message.error('预约失败')
    throw e
  } finally {
    scheduling.value = false
  }
}
</script>

<style scoped lang="scss">
.meeting-list-page {
  padding: 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0;
  }
}

.header-actions {
  display: flex;
  gap: 12px;
}

.meeting-table {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.summary-loading {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-subject {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.summary-overview {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.8;
  color: #4a4a68;
}

.summary-block {
  margin-bottom: 16px;
}

.summary-block-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #2f54eb;
}

.summary-block ul {
  margin: 0;
  padding-left: 20px;
}

.summary-block li {
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #4a4a68;
  word-break: break-word;
}

.action-owner {
  margin-left: 6px;
  color: #2f54eb;
  font-size: 12px;
}

.action-due {
  margin-left: 6px;
  color: #8d8dab;
  font-size: 12px;
}

.summary-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.summary-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
