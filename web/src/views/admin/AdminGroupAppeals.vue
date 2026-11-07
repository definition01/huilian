<template>
  <div class="admin-appeals">
    <div class="page-header">
      <h2>群聊申诉</h2>
      <div class="header-actions">
        <n-select v-model:value="statusFilter" :options="statusOptions" style="width: 140px" @update:value="handleSearch" />
        <n-button type="primary" ghost :loading="loading" @click="loadData">刷新</n-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NDataTable, NSelect, NButton, NTag, useMessage, useDialog } from 'naive-ui'
import { getAdminGroupAppeals, reviewAdminGroupAppeal } from '@/api/admin'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const rows = ref([])
const statusFilter = ref('PENDING')

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '全部', value: '' }
]

const statusMap = {
  PENDING: { text: '待处理', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'error' }
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizeChanger: true,
  pageSizes: [10, 20, 30]
})

const columns = [
  { title: '群名称', key: 'groupName', ellipsis: { tooltip: true } },
  { title: '群号', key: 'groupNo', width: 120 },
  { title: '群主', key: 'ownerName', width: 110 },
  {
    title: '申诉理由',
    key: 'reason',
    ellipsis: { tooltip: true },
    render: row => row.reason || '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => {
      const meta = statusMap[row.status] || { text: row.status, type: 'default' }
      return h(NTag, { size: 'small', type: meta.type, bordered: false }, { default: () => meta.text })
    }
  },
  {
    title: '申诉时间',
    key: 'createdAt',
    width: 170,
    render: row => row.createdAt || '-'
  },
  {
    title: '处理备注',
    key: 'adminRemark',
    ellipsis: { tooltip: true },
    render: row => row.adminRemark || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render: row => {
      if (row.status !== 'PENDING') {
        return h('span', { class: 'processed' }, '已处理')
      }
      return h('div', { class: 'action-btns' }, [
        h(NButton, {
          size: 'small',
          type: 'success',
          quaternary: true,
          onClick: () => handleReview(row, true)
        }, { default: () => '通过并解封' }),
        h(NButton, {
          size: 'small',
          type: 'error',
          quaternary: true,
          onClick: () => handleReview(row, false)
        }, { default: () => '驳回' })
      ])
    }
  }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminGroupAppeals({
      current: pagination.page,
      size: pagination.pageSize,
      status: statusFilter.value || undefined
    })
    rows.value = res.data || []
    pagination.itemCount = res.pagination?.totalItems || 0
  } catch (e) {
    message.error(e.message || '获取申诉列表失败')
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

function handleReview(row, approve) {
  dialog.warning({
    title: approve ? '通过申诉' : '驳回申诉',
    content: approve
      ? `确定通过群聊「${row.groupName}」的申诉并解除封禁吗？`
      : `确定驳回群聊「${row.groupName}」的申诉吗？群聊将保持封禁状态。`,
    positiveText: approve ? '通过并解封' : '驳回',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await reviewAdminGroupAppeal(row.id, approve)
        message.success(approve ? '申诉已通过，群聊已解封' : '申诉已驳回')
        loadData()
      } catch (e) {
        message.error(e.message || '处理申诉失败')
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
  gap: 2px;
}

.processed {
  color: #9ca3af;
  font-size: 13px;
}
</style>
