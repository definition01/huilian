<template>
  <div class="admin-groups">
    <div class="page-header">
      <h2>群聊管理</h2>
      <div class="header-actions">
        <n-input v-model:value="filters.keyword" placeholder="搜索群名称/群号" clearable style="width: 220px" @keyup.enter="handleSearch" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NDataTable, NInput, NButton, NTag, useMessage, useDialog } from 'naive-ui'
import { getAdminGroups, deleteAdminGroup, updateAdminGroupStatus, joinAdminGroup } from '@/api/admin'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const rows = ref([])
const filters = reactive({ keyword: '' })

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizeChanger: true,
  pageSizes: [10, 20, 30]
})

const columns = [
  { title: '群名称', key: 'groupName', ellipsis: { tooltip: true } },
  { title: '群号', key: 'groupNo', width: 130 },
  {
    title: '群主',
    key: 'ownerName',
    width: 120,
    render: row => row.ownerName || '-'
  },
  {
    title: '成员数',
    key: 'memberCount',
    width: 90,
    render: row => row.memberCount ?? 0
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => {
      const banned = row.status === 'banned'
      return h(NTag, { size: 'small', type: banned ? 'error' : 'success', bordered: false },
        { default: () => banned ? '已封禁' : '正常' })
    }
  },
  {
    title: '群公告',
    key: 'announcement',
    ellipsis: { tooltip: true },
    render: row => row.announcement || '-'
  },
  { title: '创建时间', key: 'createdAt', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: row => h('div', { class: 'action-btns' }, [
      h(NButton, {
        size: 'small',
        type: 'primary',
        quaternary: true,
        onClick: () => handleEnter(row)
      }, { default: () => '进入群聊' }),
      h(NButton, {
        size: 'small',
        type: row.status === 'banned' ? 'success' : 'warning',
        quaternary: true,
        onClick: () => handleToggleStatus(row)
      }, { default: () => row.status === 'banned' ? '解封' : '封禁' }),
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
    const res = await getAdminGroups({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: filters.keyword || undefined
    })
    rows.value = res.data || []
    pagination.itemCount = res.pagination?.totalItems || 0
  } catch (e) {
    message.error(e.message || '获取群聊列表失败')
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

async function handleEnter(row) {
  const win = window.open('', '_blank')
  try {
    await joinAdminGroup(row.id)
    if (win) {
      win.location.href = `/groups/${row.id}`
    } else {
      message.warning('浏览器拦截了新窗口，请允许弹窗后重试')
    }
  } catch (e) {
    if (win) win.close()
    message.error(e.message || '进入群聊失败')
  }
}

function handleToggleStatus(row) {
  const banned = row.status !== 'banned'
  dialog.warning({
    title: banned ? '封禁群聊' : '解封群聊',
    content: banned
      ? `确定封禁群聊「${row.groupName}」吗？封禁后成员无法发送消息、上传文件或邀请新成员。`
      : `确定解封群聊「${row.groupName}」吗？解封后群聊恢复正常使用。`,
    positiveText: banned ? '封禁' : '解封',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await updateAdminGroupStatus(row.id, banned)
        message.success(banned ? '群聊已封禁' : '群聊已解封')
        loadData()
      } catch (e) {
        message.error(e.message || '操作失败')
      }
    }
  })
}

function handleDelete(row) {
  dialog.warning({
    title: '删除群聊',
    content: `确定删除群聊「${row.groupName}」吗？删除后群成员将被移除，且不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminGroup(row.id)
        message.success('群聊已删除')
        if (rows.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1
        }
        loadData()
      } catch (e) {
        message.error(e.message || '删除群聊失败')
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
</style>
