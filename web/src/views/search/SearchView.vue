<template>
  <div class="search-page">
    <div class="page-header">
      <div>
        <h1>搜索</h1>
        <p>搜索用户添加好友，或通过群号加入群聊</p>
      </div>
    </div>

    <div class="segment">
      <button
        type="button"
        :class="{ active: tab === 'user' }"
        @click="tab = 'user'"
      >
        用户
      </button>
      <button
        type="button"
        :class="{ active: tab === 'group' }"
        @click="tab = 'group'"
      >
        群聊
      </button>
    </div>

    <div v-if="tab === 'user'" class="panel">
      <div class="panel-title">添加好友</div>
      <div class="search-row">
        <n-input
          v-model:value="userKeyword"
          placeholder="输入手机号或用户名，用户名支持模糊搜索"
          @keydown.enter="searchUser"
        />
        <n-button type="primary" :loading="searchingUser" @click="searchUser">搜索</n-button>
      </div>

      <div v-if="userResults.length" class="result-list">
        <div v-for="user in userResults" :key="user.userId" class="result-row">
          <img v-if="user.avatar" :src="resolveAvatar(user.avatar)" class="avatar-img" alt="" />
          <div v-else class="avatar">{{ user.username?.[0] || 'U' }}</div>
          <div class="info">
            <div class="name">{{ user.username }}</div>
            <div class="sub">{{ user.mobile || '未绑定手机号' }}</div>
          </div>
          <n-tag v-if="user.relation === 'FRIEND'" size="small" type="success">已是好友</n-tag>
          <n-tag v-else-if="user.relation === 'REQUEST_SENT'" size="small" type="warning">已申请</n-tag>
          <n-tag v-else-if="user.relation === 'REQUEST_RECEIVED'" size="small" type="info">对方已申请</n-tag>
          <n-button v-else size="small" type="primary" @click="openAddDialog(user)">加好友</n-button>
        </div>
      </div>
      <div v-else-if="searchedUser" class="empty-tip">没有找到匹配的用户</div>
    </div>

    <div v-else class="panel">
      <div class="panel-title">通过群号加入群聊</div>
      <div class="search-row">
        <n-input
          v-model:value="groupNo"
          maxlength="10"
          placeholder="请输入10位群号"
          @keydown.enter="searchGroup"
        />
        <n-button type="primary" :loading="searchingGroup" @click="searchGroup">搜索</n-button>
      </div>

      <div v-if="groupResult" class="group-result">
        <div class="avatar group-avatar">{{ groupResult.groupName?.[0] || 'G' }}</div>
        <div class="info">
          <div class="name">{{ groupResult.groupName }}</div>
          <div class="sub">
            群号 {{ groupResult.groupNo }} · {{ groupResult.memberCount }} 名成员 · 群主 {{ groupResult.ownerName }}
          </div>
        </div>
        <n-button
          v-if="groupResult.joined"
          size="small"
          type="primary"
          ghost
          @click="router.push(`/groups/${groupResult.id}`)"
        >
          进入群聊
        </n-button>
        <n-tag v-else-if="groupResult.pending" size="small" type="warning">审核中</n-tag>
        <n-button v-else size="small" type="primary" :loading="applying" @click="handleApplyGroup">申请加入</n-button>
      </div>
      <div v-else-if="searchedGroup" class="empty-tip">未找到该群聊，请检查群号</div>
    </div>

    <n-modal
      v-model:show="showAddDialog"
      preset="card"
      title="发送好友申请"
      class="dialog-card"
      :style="{ width: '420px', maxWidth: '92vw' }"
    >
      <div class="dialog-tip">添加「{{ addTarget?.username }}」为好友</div>
      <n-input
        v-model:value="applyMessage"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        maxlength="100"
        show-count
        placeholder="附言（选填），例如：我是 xxx"
      />
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showAddDialog = false">取消</n-button>
          <n-button type="primary" :loading="sendingRequest" @click="handleSendRequest">发送申请</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NInput, NTag, NModal, useMessage } from 'naive-ui'
import { searchGroupByNo, applyJoinGroup } from '@/api/group'
import { searchUsers, sendFriendRequest } from '@/api/friend'
import { resolveAvatar } from '@/utils/avatar'

const router = useRouter()
const message = useMessage()

const tab = ref('user')

const userKeyword = ref('')
const userResults = ref([])
const searchedUser = ref(false)
const searchingUser = ref(false)

const groupNo = ref('')
const groupResult = ref(null)
const searchedGroup = ref(false)
const searchingGroup = ref(false)
const applying = ref(false)

const showAddDialog = ref(false)
const addTarget = ref(null)
const applyMessage = ref('')
const sendingRequest = ref(false)

async function searchUser() {
  const value = userKeyword.value.trim()
  if (!value) {
    message.warning('请输入手机号或用户名')
    return
  }

  searchingUser.value = true
  try {
    const res = await searchUsers(value)
    userResults.value = res.data || []
    searchedUser.value = true
  } catch (e) {
    userResults.value = []
    message.error(e?.message || '搜索失败')
  } finally {
    searchingUser.value = false
  }
}

function openAddDialog(user) {
  addTarget.value = user
  applyMessage.value = ''
  showAddDialog.value = true
}

async function handleSendRequest() {
  if (!addTarget.value) return
  sendingRequest.value = true
  try {
    await sendFriendRequest({
      toUserId: addTarget.value.userId,
      applyMessage: applyMessage.value.trim()
    })
    message.success('好友申请已发送，等待对方同意')
    const target = userResults.value.find(user => user.userId === addTarget.value.userId)
    if (target) {
      target.relation = 'REQUEST_SENT'
    }
    showAddDialog.value = false
  } catch (e) {
    message.error(e?.message || '发送申请失败')
  } finally {
    sendingRequest.value = false
  }
}

async function searchGroup() {
  const value = groupNo.value.trim()
  if (!/^\d{10}$/.test(value)) {
    message.warning('群号必须是10位数字')
    return
  }

  searchingGroup.value = true
  try {
    const res = await searchGroupByNo(value)
    groupResult.value = res.data
    searchedGroup.value = true
  } catch (e) {
    groupResult.value = null
    message.error(e?.message || '搜索失败')
  } finally {
    searchingGroup.value = false
  }
}

async function handleApplyGroup() {
  applying.value = true
  try {
    await applyJoinGroup({ groupNo: groupNo.value.trim() })
    message.success('申请已提交，请等待群主审核')
    if (groupResult.value) {
      groupResult.value = { ...groupResult.value, pending: true }
    }
  } catch (e) {
    message.error(e?.message || '申请失败')
  } finally {
    applying.value = false
  }
}
</script>

<style scoped lang="scss">
.search-page {
  padding: 32px;
  max-width: 900px;
}

.page-header {
  margin-bottom: 20px;

  h1 {
    margin: 0;
    font-size: 24px;
    color: #1f2937;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.segment {
  display: inline-flex;
  padding: 4px;
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;

  button {
    min-width: 88px;
    padding: 7px 16px;
    border: none;
    border-radius: 7px;
    background: transparent;
    color: #6b7280;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      background: #006eff;
      color: #fff;
    }
  }
}

.panel {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.panel-title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.search-row {
  display: flex;
  gap: 10px;
}

.empty-tip {
  padding-top: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.result-row,
.group-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}

.group-result {
  margin-top: 12px;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #006eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.group-avatar {
  background: #10b981;
}

.avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #f1f5f9;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-card {
  width: 380px;
}

.dialog-tip {
  margin-bottom: 12px;
  font-size: 13px;
  color: #6b7280;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
