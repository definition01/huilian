<template>
  <div class="admin-users">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <n-input v-model:value="filters.keyword" placeholder="搜索用户名/手机号/邮箱" clearable style="width: 230px" @keyup.enter="handleSearch" />
        <n-select v-model:value="filters.enabled" :options="statusOptions" placeholder="状态筛选" clearable style="width: 140px" @update:value="handleSearch" />
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
import { NDataTable, NInput, NSelect, NButton, NTag, NSwitch, useMessage, useDialog } from 'naive-ui'
import { getAdminUsers, updateAdminUserStatus } from '@/api/admin'
import { useUserStore } from '@/store/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

const loading = ref(false)
const rows = ref([])
const filters = reactive({ keyword: '', enabled: null })

const statusOptions = [
  { label: '已启用', value: true },
  { label: '已禁用', value: false }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizeChanger: true,
  pageSizes: [10, 20, 30]
})

const columns = [
  { title: '用户名', key: 'username', width: 120, ellipsis: { tooltip: true } },
  { title: '手机号', key: 'mobile', width: 150 },
  { title: '邮箱', key: 'email', ellipsis: { tooltip: true } },
  {
    title: '角色',
    key: 'role',
    width: 100,
    render: row => {
      const isAdminRole = row.role === 'admin' || row.role === 'SUPER_ADMIN'
      return h(NTag, { size: 'small', type: isAdminRole ? 'info' : 'default', bordered: false },
        { default: () => isAdminRole ? '管理员' : '普通用户' })
    }
  },
  {
    title: '状态',
    key: 'enabled',
    width: 90,
    render: row => h(NSwitch, {
      value: row.enabled,
      size: 'small',
      onUpdateValue: value => handleToggle(row, value)
    })
  },
  { title: '注册时间', key: 'createdAt', width: 170 },
  {
    title: '最后登录',
    key: 'lastLoginAt',
    width: 170,
    render: row => row.lastLoginAt || '-'
  }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminUsers({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: filters.keyword || undefined,
      enabled: filters.enabled === null || filters.enabled === '' ? undefined : filters.enabled
    })
    rows.value = res.data || []
    pagination.itemCount = res.pagination?.totalItems || 0
  } catch (e) {
    message.error(e.message || '获取用户列表失败')
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

function handleToggle(row, enabled) {
  if (row.id === userStore.userInfo?.id) {
    message.warning('不能禁用当前登录的管理员账号')
    return
  }
  dialog.warning({
    title: enabled ? '启用用户' : '禁用用户',
    content: enabled
      ? `确定启用用户「${row.username}」吗？`
      : `确定禁用用户「${row.username}」吗？禁用后该用户将无法登录。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await updateAdminUserStatus(row.id, enabled)
        row.enabled = enabled
        message.success(enabled ? '用户已启用' : '用户已禁用')
      } catch (e) {
        message.error(e.message || '更新用户状态失败')
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
</style>
