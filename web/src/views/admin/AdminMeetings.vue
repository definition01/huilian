<template>
  <div class="admin-meetings">
    <div class="page-header">
      <h2>会议管理</h2>
      <div class="header-actions">
        <n-input v-model:value="filters.keyword" placeholder="搜索会议主题/会议号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <n-select v-model:value="filters.status" :options="statusOptions" placeholder="状态筛选" clearable style="width: 140px" @update:value="handleSearch" />
        <n-button type="primary" ghost @click="handleSearch">搜索</n-button>
      </div>
    </div>

    <div class="table-card">
      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <n-modal
      v-model:show="showSummary"
      preset="card"
      :title="summaryTitle"
      style="width: 680px; max-width: 92vw"
      :bordered="false"
    >
      <n-spin :show="summaryLoading">
        <div v-if="summaryData && summaryData.status === 'success'" class="summary-content">
          <div class="summary-head">
            <span class="summary-title">{{ summaryData.title || '会议纪要' }}</span>
            <span v-if="summaryData.generatedAt" class="summary-time">生成时间：{{ summaryData.generatedAt }}</span>
          </div>
          <p v-if="summaryData.overview" class="summary-overview">{{ summaryData.overview }}</p>

          <div v-if="summaryData.keyPoints?.length" class="summary-block">
            <div class="summary-block-title">关键要点</div>
            <ul>
              <li v-for="(point, idx) in summaryData.keyPoints" :key="'kp-' + idx">{{ point }}</li>
            </ul>
          </div>

          <div v-if="summaryData.decisions?.length" class="summary-block">
            <div class="summary-block-title">决议事项</div>
            <ul>
              <li v-for="(item, idx) in summaryData.decisions" :key="'dc-' + idx">{{ item }}</li>
            </ul>
          </div>

          <div v-if="summaryData.actionItems?.length" class="summary-block">
            <div class="summary-block-title">待办事项</div>
            <ul>
              <li v-for="(item, idx) in summaryData.actionItems" :key="'ac-' + idx">
                {{ item.task }}
                <span v-if="item.owner" class="action-owner">@{{ item.owner }}</span>
                <span v-if="item.dueDate" class="action-due">{{ item.dueDate }}</span>
              </li>
            </ul>
          </div>

          <div v-if="summaryData.keywords?.length" class="summary-keywords">
            <n-tag v-for="(kw, idx) in summaryData.keywords" :key="'kw-' + idx" size="small" round :bordered="false">
              {{ kw }}
            </n-tag>
          </div>
        </div>

        <div v-else-if="summaryData && summaryData.status === 'failed'" class="summary-empty summary-failed">
          {{ summaryData.errorMessage || '会议纪要生成失败' }}
        </div>

        <div v-else-if="summaryData && ['pending', 'generating'].includes(summaryData.status)" class="summary-empty">
          AI 正在整理会议内容，请稍后再查看…
        </div>

        <div v-else-if="!summaryLoading" class="summary-empty">该会议暂无会议纪要</div>
      </n-spin>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { NDataTable, NInput, NSelect, NButton, NTag, NModal, NSpin, useMessage, useDialog } from 'naive-ui'
import { getAdminMeetings, deleteAdminMeeting, getAdminMeetingSummary } from '@/api/admin'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const rows = ref([])
const filters = reactive({ keyword: '', status: null })

const showSummary = ref(false)
const summaryLoading = ref(false)
const summaryData = ref(null)
const summaryMeeting = ref(null)

const summaryTitle = computed(() =>
  summaryMeeting.value?.topic ? `${summaryMeeting.value.topic} - 会议纪要` : '会议纪要'
)

const statusOptions = [
  { label: '等待中', value: 'waiting' },
  { label: '进行中', value: 'ongoing' },
  { label: '已结束', value: 'ended' },
  { label: '已取消', value: 'cancelled' }
]

const statusMeta = {
  waiting: { text: '等待中', type: 'warning' },
  ongoing: { text: '进行中', type: 'success' },
  ended: { text: '已结束', type: 'default' },
  cancelled: { text: '已取消', type: 'error' }
}

const summaryMeta = {
  success: { text: '已有纪要', type: 'success' },
  generating: { text: '生成中', type: 'info' },
  pending: { text: '待生成', type: 'warning' },
  failed: { text: '生成失败', type: 'error' }
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizeChanger: true,
  pageSizes: [10, 20, 30]
})

const columns = [
  { title: '会议主题', key: 'topic', ellipsis: { tooltip: true } },
  { title: '会议号', key: 'meetingNumber', width: 120 },
  {
    title: '主持人',
    key: 'hostUsername',
    width: 110,
    render: row => row.hostUsername || '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => {
      const meta = statusMeta[row.status] || { text: row.status, type: 'default' }
      return h(NTag, { size: 'small', type: meta.type, bordered: false }, { default: () => meta.text })
    }
  },
  {
    title: '会议纪要',
    key: 'summaryStatus',
    width: 110,
    render: row => {
      const meta = summaryMeta[row.summaryStatus] || { text: '无纪要', type: 'default' }
      return h(NTag, { size: 'small', type: meta.type, bordered: false }, { default: () => meta.text })
    }
  },
  { title: '创建时间', key: 'createdAt', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render: row => h('div', { class: 'action-btns' }, [
      h(NButton, {
        size: 'small',
        type: 'primary',
        quaternary: true,
        disabled: !row.summaryId,
        onClick: () => handleViewSummary(row)
      }, { default: () => '查看纪要' }),
      h(NButton, {
        size: 'small',
        type: 'error',
        quaternary: true,
        onClick: () => handleDelete(row)
      }, { default: () => '删除' })
    ])
  }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminMeetings({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    rows.value = res.data || []
    pagination.itemCount = res.pagination?.totalItems || 0
  } catch (e) {
    message.error(e.message || '获取会议列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handlePageChange(page) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadData()
}

async function handleViewSummary(row) {
  summaryMeeting.value = row
  summaryData.value = null
  showSummary.value = true
  summaryLoading.value = true
  try {
    const res = await getAdminMeetingSummary(row.id)
    summaryData.value = res.data || null
  } catch (e) {
    message.error(e.message || '获取会议纪要失败')
  } finally {
    summaryLoading.value = false
  }
}

function handleDelete(row) {
  dialog.warning({
    title: '删除会议',
    content: `确定删除会议「${row.topic}」吗？删除后不可恢复，进行中的会议会立即结束。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminMeeting(row.id)
        message.success('会议已删除')
        if (rows.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1
        }
        loadData()
      } catch (e) {
        message.error(e.message || '删除会议失败')
      }
    }
  })
}

onMounted(loadData)
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 8px 4px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.summary-content {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.summary-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.summary-overview {
  font-size: 14px;
  line-height: 1.8;
  color: #4b5563;
  margin-bottom: 16px;
}

.summary-block {
  margin-bottom: 16px;
}

.summary-block-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.summary-block ul {
  margin: 0;
  padding-left: 20px;
}

.summary-block li {
  font-size: 13px;
  line-height: 1.9;
  color: #4b5563;
}

.action-owner {
  color: #006EFF;
  margin-left: 6px;
}

.action-due {
  color: #9ca3af;
  margin-left: 6px;
  font-size: 12px;
}

.summary-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 4px;
}

.summary-empty {
  padding: 32px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.summary-failed {
  color: #d03050;
}
</style>
