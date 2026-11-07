<template>
  <div class="chat-page">
    <header class="chat-header">
      <button class="back-btn" @click="goBack">←</button>
      <div class="chat-title">
        <h2>{{ group.groupName || '群聊' }}</h2>
        <span>
          <span class="group-no" title="点击复制群号" @click="copyGroupNo">群号 {{ group.groupNo || '-' }}</span>
          · {{ members.length }} 名成员<template v-if="group.ownerName"> · 群主 {{ group.ownerName }}</template>
        </span>
      </div>
      <div class="chat-actions">
        <n-button size="small" @click="openMembers">
          成员<template v-if="isOwner && joinRequests.length">（{{ joinRequests.length }}）</template>
        </n-button>
        <n-button v-if="isOwner" size="small" type="error" ghost @click="handleDissolve">解散</n-button>
        <n-button v-else size="small" type="error" ghost @click="handleLeave">退出</n-button>
      </div>
    </header>

    <n-alert v-if="isBanned" type="error" :bordered="false" class="banned-alert">
      <div class="banned-content">
        <span>该群聊已被管理员封禁，暂时无法发送消息或上传文件</span>
        <template v-if="isOwner">
          <n-tag v-if="appeal && appeal.status === 'PENDING'" size="small" type="warning" :bordered="false">
            申诉审核中
          </n-tag>
          <n-button v-else size="tiny" type="primary" ghost @click="openAppeal">
            {{ appeal && appeal.status === 'REJECTED' ? '重新申诉' : '申诉' }}
          </n-button>
        </template>
      </div>
      <div v-if="isOwner && appeal && appeal.status === 'REJECTED' && appeal.adminRemark" class="appeal-remark">
        驳回原因：{{ appeal.adminRemark }}
      </div>
    </n-alert>

    <div ref="messageListRef" class="message-list">
      <div v-if="hasMore" class="load-more">
        <n-button text :loading="loadingMore" @click="loadOlder">加载更早的消息</n-button>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-row', { mine: isMine(msg), system: msg.messageType === 'SYSTEM' }]"
      >
        <div v-if="msg.messageType === 'SYSTEM'" class="system-msg">{{ msg.content }}</div>
        <template v-else>
          <div v-if="!isMine(msg)" class="msg-avatar">
            <img v-if="msg.senderAvatar" :src="resolveAvatar(msg.senderAvatar)" alt="" />
            <span v-else>{{ msg.senderName?.[0] || 'U' }}</span>
          </div>
          <div class="msg-body">
            <div class="msg-meta">{{ msg.senderName }} · {{ formatTime(msg.createdAt) }}</div>
            <n-image
              v-if="msg.messageType === 'IMAGE'"
              :src="msg.fileUrl"
              width="220"
              class="msg-image"
              object-fit="contain"
            />
            <a
              v-else-if="msg.messageType === 'FILE'"
              class="file-card"
              :href="msg.fileUrl"
              :download="msg.fileName"
              target="_blank"
              rel="noopener"
            >
              <span class="file-icon">FILE</span>
              <span class="file-info">
                <span class="file-name">{{ msg.fileName || '未命名文件' }}</span>
                <span class="file-size">{{ formatSize(msg.fileSize) }}</span>
              </span>
            </a>
            <div v-else class="bubble">{{ msg.content }}</div>
          </div>
        </template>
      </div>

      <n-empty v-if="!messages.length && !loading" description="暂无消息，来说点什么吧" class="empty" />
    </div>

    <footer class="chat-input">
      <div class="input-tools">
        <button class="tool-btn" type="button" :disabled="uploading || isBanned" @click="pickImage">图片</button>
        <button class="tool-btn" type="button" :disabled="uploading || isBanned" @click="pickFile">文件</button>
        <input ref="imageInputRef" type="file" accept="image/*" hidden @change="handleImageSelected" />
        <input ref="fileInputRef" type="file" hidden @change="handleFileSelected" />
      </div>
      <n-input
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :disabled="isBanned"
        :placeholder="isBanned ? '群聊已被封禁，无法发送消息' : '输入消息，Enter 发送，Shift + Enter 换行'"
        @keydown="handleKeydown"
      />
      <n-button type="primary" :loading="sending || uploading" :disabled="isBanned || !draft.trim()" @click="handleSend">发送</n-button>
    </footer>

    <n-drawer v-model:show="showMembers" :width="320" placement="right">
      <n-drawer-content title="群成员">
        <div v-for="member in members" :key="member.userId" class="member-row">
          <div class="member-avatar">
            <img v-if="member.avatar" :src="resolveAvatar(member.avatar)" alt="" />
            <span v-else>{{ member.username?.[0] || 'U' }}</span>
          </div>
          <div class="member-info">
            <span>{{ member.username }}</span>
            <n-tag v-if="member.roleType === 'owner'" size="small" type="success">群主</n-tag>
          </div>
          <div class="member-actions">
            <n-tag v-if="Number(member.userId) === Number(currentUserId)" size="small">我</n-tag>
            <n-tag v-else-if="member.relation === 'FRIEND'" size="small" type="success">好友</n-tag>
            <n-tag v-else-if="member.relation === 'REQUEST_SENT'" size="small" type="warning">已申请</n-tag>
            <n-tag v-else-if="member.relation === 'REQUEST_RECEIVED'" size="small" type="info">对方已申请</n-tag>
            <n-button
              v-else
              size="tiny"
              type="primary"
              ghost
              @click="openAddFriend(member)"
            >
              加好友
            </n-button>
            <n-button
              v-if="isOwner && member.roleType !== 'owner'"
              size="tiny"
              quaternary
              type="error"
              @click="handleRemoveMember(member)"
            >
              移除
            </n-button>
          </div>
        </div>

        <div class="invite-entry" @click="openInvite">
          <div class="invite-plus">+</div>
          <span>邀请好友</span>
        </div>

        <div v-if="isOwner" class="join-requests">
          <div class="section-title">入群申请<template v-if="joinRequests.length">（{{ joinRequests.length }}）</template></div>
          <div v-if="!joinRequests.length" class="empty-tip">暂无待审核申请</div>
          <div v-for="req in joinRequests" :key="req.id" class="member-row">
            <div class="member-avatar">
              <img v-if="req.avatar" :src="resolveAvatar(req.avatar)" alt="" />
              <span v-else>{{ req.username?.[0] || 'U' }}</span>
            </div>
            <div class="member-info">
              <span>{{ req.username }}</span>
              <span v-if="req.inviterName" class="req-source">由 {{ req.inviterName }} 邀请</span>
            </div>
            <div class="req-actions">
              <n-button size="tiny" type="primary" :loading="reviewingId === req.id" @click="handleReview(req, true)">通过</n-button>
              <n-button size="tiny" quaternary type="error" :loading="reviewingId === req.id" @click="handleReview(req, false)">拒绝</n-button>
            </div>
          </div>
        </div>

      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="showAddFriend"
      preset="card"
      title="发送好友申请"
      :style="{ width: '420px', maxWidth: '92vw' }"
    >
      <div class="dialog-tip">添加「{{ addFriendTarget?.username }}」为好友</div>
      <n-input
        v-model:value="applyMessage"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        maxlength="100"
        show-count
        placeholder="附言（选填），例如：我们在同一个群里"
      />
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showAddFriend = false">取消</n-button>
          <n-button type="primary" :loading="sendingFriendRequest" @click="handleSendFriendRequest">
            发送申请
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="showInvite"
      preset="card"
      title="邀请好友入群"
      :style="{ width: '420px', maxWidth: '92vw' }"
    >
      <div class="dialog-tip">
        {{ isOwner ? '邀请的好友将直接加入群聊' : '你是普通成员，邀请后需要群主审核通过才能进群' }}
      </div>
      <div v-if="loadingCandidates" class="invite-loading">加载中...</div>
      <div v-else-if="inviteCandidates.length" class="invite-list">
        <div
          v-for="friend in inviteCandidates"
          :key="friend.userId"
          class="invite-row"
          @click="toggleInvite(friend.userId)"
        >
          <img v-if="friend.avatar" :src="resolveAvatar(friend.avatar)" class="invite-avatar" alt="" />
          <div v-else class="invite-avatar invite-avatar-text">
            {{ (friend.remark || friend.username)?.[0] || 'U' }}
          </div>
          <div class="invite-name">{{ friend.remark || friend.username }}</div>
          <n-checkbox
            :checked="selectedInviteIds.includes(Number(friend.userId))"
            @update:checked="toggleInvite(friend.userId)"
            @click.stop
          />
        </div>
      </div>
      <n-empty v-else description="暂无可邀请的好友" />
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showInvite = false">取消</n-button>
          <n-button
            type="primary"
            :loading="inviting"
            :disabled="!selectedInviteIds.length"
            @click="handleInvite"
          >
            邀请
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="showAppeal"
      preset="card"
      title="群聊封禁申诉"
      :style="{ width: '480px', maxWidth: '92vw' }"
    >
      <div class="dialog-tip">
        群聊「{{ group.groupName }}」已被封禁，可向管理员提交申诉说明，审核通过后将自动解封。
      </div>
      <n-input
        v-model:value="appealReason"
        type="textarea"
        :autosize="{ minRows: 4, maxRows: 6 }"
        maxlength="500"
        show-count
        placeholder="请填写申诉理由，说明群聊的使用情况和整改措施"
      />
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showAppeal = false">取消</n-button>
          <n-button type="primary" :loading="submittingAppeal" :disabled="!appealReason.trim()" @click="handleSubmitAppeal">
            提交申诉
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NInput,
  NEmpty,
  NTag,
  NImage,
  NAlert,
  NDrawer,
  NDrawerContent,
  NModal,
  NCheckbox,
  useMessage,
  useDialog
} from 'naive-ui'
import {
  getGroupDetail,
  getGroupMembers,
  getGroupMessages,
  sendGroupMessage,
  uploadGroupFile,
  inviteGroupMembers,
  removeGroupMember,
  dissolveGroup,
  leaveGroup,
  getJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  submitGroupAppeal,
  getGroupAppeal
} from '@/api/group'
import { markChatRead } from '@/api/chat'
import { getFriends, sendFriendRequest } from '@/api/friend'
import { resolveAvatar } from '@/utils/avatar'
import { useWebSocket } from '@/composables/useWebSocket'
import { useUserStore } from '@/store/user'

const PAGE_SIZE = 30

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const { connect, disconnect } = useWebSocket()

const groupId = Number(route.params.groupId)
const group = ref({})
const members = ref([])
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const uploading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const showMembers = ref(false)
const joinRequests = ref([])
const reviewingId = ref(null)
const showInvite = ref(false)
const appeal = ref(null)
const showAppeal = ref(false)
const appealReason = ref('')
const submittingAppeal = ref(false)
const inviteCandidates = ref([])
const selectedInviteIds = ref([])
const inviting = ref(false)
const loadingCandidates = ref(false)
const messageListRef = ref(null)
const imageInputRef = ref(null)
const fileInputRef = ref(null)
const showAddFriend = ref(false)
const addFriendTarget = ref(null)
const applyMessage = ref('')
const sendingFriendRequest = ref(false)

const MAX_FILE_SIZE = 15 * 1024 * 1024

const isOwner = computed(() => group.value.myRole === 'owner')
const isBanned = computed(() => group.value.status === 'banned')
const currentUserId = computed(() => userStore.userInfo?.id)

onMounted(async () => {
  window.addEventListener('group-message', handleGroupMessageEvent)
  connect('')
  await loadGroup()
  await Promise.all([loadMembers(), loadMessages()])
  await loadJoinRequests()
})

onUnmounted(() => {
  window.removeEventListener('group-message', handleGroupMessageEvent)
  disconnect()
})

function goBack() {
  router.push('/chats')
}

async function loadGroup() {
  try {
    const res = await getGroupDetail(groupId)
    group.value = res.data || {}
    if (isBanned.value && isOwner.value) {
      await loadAppeal()
    }
  } catch (e) {
    message.error(e?.message || '加载群聊失败')
  }
}

async function loadAppeal() {
  try {
    const res = await getGroupAppeal(groupId)
    appeal.value = res.data || null
  } catch (e) {
    appeal.value = null
  }
}

function openAppeal() {
  appealReason.value = ''
  showAppeal.value = true
}

async function handleSubmitAppeal() {
  const reason = appealReason.value.trim()
  if (!reason) {
    message.warning('请填写申诉理由')
    return
  }
  submittingAppeal.value = true
  try {
    await submitGroupAppeal(groupId, { reason })
    showAppeal.value = false
    message.success('申诉已提交，请等待管理员审核')
    await loadAppeal()
  } catch (e) {
    message.error(e?.message || '提交申诉失败')
  } finally {
    submittingAppeal.value = false
  }
}

async function loadMembers() {
  try {
    const res = await getGroupMembers(groupId)
    members.value = res.data || []
  } catch (e) {
    message.error(e?.message || '加载群成员失败')
  }
}

async function loadJoinRequests() {
  if (!isOwner.value) {
    joinRequests.value = []
    return
  }
  try {
    const res = await getJoinRequests(groupId)
    joinRequests.value = res.data || []
  } catch (e) {
    joinRequests.value = []
  }
}

function openMembers() {
  showMembers.value = true
  loadJoinRequests()
}

function copyGroupNo() {
  const groupNo = group.value.groupNo
  if (!groupNo) return
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(groupNo).then(() => message.success('群号已复制'))
  } else {
    message.info(`群号：${groupNo}`)
  }
}

function openAddFriend(member) {
  if (Number(member.userId) === Number(currentUserId.value)) return
  addFriendTarget.value = member
  applyMessage.value = ''
  showAddFriend.value = true
}

async function handleSendFriendRequest() {
  if (!addFriendTarget.value || sendingFriendRequest.value) return
  sendingFriendRequest.value = true
  try {
    await sendFriendRequest({
      toUserId: addFriendTarget.value.userId,
      applyMessage: applyMessage.value.trim()
    })
    message.success('好友申请已发送，等待对方同意')
    addFriendTarget.value.relation = 'REQUEST_SENT'
    showAddFriend.value = false
  } catch (e) {
    message.error(e?.message || '发送申请失败')
  } finally {
    sendingFriendRequest.value = false
  }
}

async function handleReview(req, approve) {
  if (reviewingId.value) return
  reviewingId.value = req.id
  try {
    if (approve) {
      await approveJoinRequest(groupId, req.id)
      message.success('已通过入群申请')
    } else {
      await rejectJoinRequest(groupId, req.id)
      message.success('已拒绝入群申请')
    }
    await Promise.all([loadJoinRequests(), loadMembers(), loadGroup()])
  } catch (e) {
    message.error(e?.message || '操作失败')
  } finally {
    reviewingId.value = null
  }
}

async function loadMessages() {
  loading.value = true
  try {
    const res = await getGroupMessages(groupId, { limit: PAGE_SIZE })
    const incoming = res.data || []
    // 合并加载期间通过 WebSocket 收到的消息，避免丢失
    const pending = messages.value.filter(msg => !incoming.some(item => item.id === msg.id))
    messages.value = [...incoming, ...pending].sort((a, b) => a.id - b.id)
    hasMore.value = incoming.length >= PAGE_SIZE
    scrollToBottom()
    markRead()
  } catch (e) {
    message.error(e?.message || '加载消息失败')
  } finally {
    loading.value = false
  }
}

async function loadOlder() {
  if (!messages.value.length || loadingMore.value) return
  loadingMore.value = true
  const beforeId = messages.value[0].id
  const el = messageListRef.value
  const prevHeight = el ? el.scrollHeight : 0

  try {
    const res = await getGroupMessages(groupId, { beforeId, limit: PAGE_SIZE })
    const older = res.data || []
    messages.value = [...older, ...messages.value].sort((a, b) => a.id - b.id)
    hasMore.value = older.length >= PAGE_SIZE

    await nextTick()
    if (el) {
      el.scrollTop = el.scrollHeight - prevHeight + el.scrollTop
    }
  } catch (e) {
    message.error(e?.message || '加载历史消息失败')
  } finally {
    loadingMore.value = false
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  if (isBanned.value) {
    message.warning('该群聊已被封禁，无法发送消息')
    return
  }

  sending.value = true
  try {
    const res = await sendGroupMessage(groupId, { content })
    pushMessage(res.data)
    draft.value = ''
  } catch (e) {
    message.error(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function handleKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  handleSend()
}

function pickImage() {
  imageInputRef.value?.click()
}

function pickFile() {
  fileInputRef.value?.click()
}

async function handleImageSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  await uploadAndSend(file, 'IMAGE')
}

async function handleFileSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  await uploadAndSend(file, 'FILE')
}

async function uploadAndSend(file, messageType) {
  if (!file || uploading.value) return
  if (file.size > MAX_FILE_SIZE) {
    message.warning('文件大小不能超过 15MB')
    return
  }
  if (messageType === 'IMAGE' && !file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }

  uploading.value = true
  try {
    const uploadRes = await uploadGroupFile(groupId, file)
    const fileUrl = uploadRes.data?.originUrl
    if (!fileUrl) {
      throw new Error('附件上传失败')
    }
    const sendRes = await sendGroupMessage(groupId, {
      messageType,
      fileUrl,
      fileName: file.name,
      fileSize: file.size
    })
    pushMessage(sendRes.data)
  } catch (e) {
    message.error(e?.message || '发送失败')
  } finally {
    uploading.value = false
  }
}

function formatSize(size) {
  if (!size) return '未知大小'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function markRead() {
  markChatRead({ type: 'GROUP', targetId: groupId }).catch(() => {})
}

function handleGroupMessageEvent(event) {
  const detail = event.detail
  if (!detail || Number(detail.groupId) !== groupId) return
  pushMessage(detail)
  if (!isMine(detail)) {
    markRead()
  }
  // 系统消息（成员加入/退出）后同步刷新成员、群信息和待审核申请
  if (detail.messageType === 'SYSTEM') {
    loadMembers()
    loadGroup()
    loadJoinRequests()
  }
}

function pushMessage(msg) {
  if (!msg || !msg.id) return
  if (messages.value.some(item => item.id === msg.id)) return
  messages.value.push(msg)
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function isMine(msg) {
  return msg.senderId != null && Number(msg.senderId) === Number(currentUserId.value)
}

async function openInvite() {
  showInvite.value = true
  selectedInviteIds.value = []
  inviteCandidates.value = []
  loadingCandidates.value = true
  try {
    const res = await getFriends()
    const memberIds = new Set(members.value.map(member => Number(member.userId)))
    inviteCandidates.value = (res.data || []).filter(friend => !memberIds.has(Number(friend.userId)))
  } catch (e) {
    message.error(e?.message || '加载好友失败')
  } finally {
    loadingCandidates.value = false
  }
}

function toggleInvite(friendId) {
  const id = Number(friendId)
  selectedInviteIds.value = selectedInviteIds.value.includes(id)
    ? selectedInviteIds.value.filter(item => item !== id)
    : [...selectedInviteIds.value, id]
}

async function handleInvite() {
  if (!selectedInviteIds.value.length || inviting.value) return
  inviting.value = true
  try {
    await inviteGroupMembers(groupId, { userIds: selectedInviteIds.value })
    message.success(isOwner.value ? '已邀请好友加入群聊' : '已发送邀请，等待群主审核')
    showInvite.value = false
    await Promise.all([loadMembers(), loadGroup()])
    if (isOwner.value) {
      await loadJoinRequests()
    }
  } catch (e) {
    message.error(e?.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

function handleRemoveMember(member) {
  dialog.warning({
    title: '移除成员',
    content: `确定将 ${member.username} 移出群聊吗？`,
    positiveText: '移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await removeGroupMember(groupId, member.userId)
        message.success('已移除')
        await Promise.all([loadMembers(), loadGroup()])
      } catch (e) {
        message.error(e?.message || '移除失败')
      }
    }
  })
}

function handleDissolve() {
  dialog.warning({
    title: '解散群聊',
    content: '解散后所有成员将无法继续聊天，确定解散吗？',
    positiveText: '解散',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await dissolveGroup(groupId)
        message.success('群聊已解散')
        router.push('/chats')
      } catch (e) {
        message.error(e?.message || '解散失败')
      }
    }
  })
}

function handleLeave() {
  dialog.warning({
    title: '退出群聊',
    content: '退出后将不再接收该群消息，确定退出吗？',
    positiveText: '退出',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await leaveGroup(groupId)
        message.success('已退出群聊')
        router.push('/chats')
      } catch (e) {
        message.error(e?.message || '退出失败')
      }
    }
  })
}

function formatTime(value) {
  if (!value) return ''
  const normalized = String(value).replace('T', ' ')
  const today = new Date()
  const datePart = normalized.slice(0, 10)
  const todayText = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  return datePart === todayText ? normalized.slice(11, 16) : normalized.slice(5, 16)
}
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafc;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.back-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 16px;
  cursor: pointer;

  &:hover {
    background: #f3f4f6;
  }
}

.chat-title {
  flex: 1;
  min-width: 0;

  h2 {
    margin: 0;
    font-size: 17px;
    color: #1f2937;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  span {
    font-size: 12px;
    color: #9ca3af;
  }
}

.group-no {
  cursor: pointer;

  &:hover {
    color: #006eff;
    text-decoration: underline;
  }
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.load-more {
  text-align: center;
  padding-bottom: 4px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;

  &.mine {
    flex-direction: row-reverse;
  }

  &.system {
    justify-content: center;
  }
}

.system-msg {
  font-size: 12px;
  color: #9ca3af;
  background: #eef2f7;
  padding: 4px 12px;
  border-radius: 10px;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  overflow: hidden;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.msg-body {
  max-width: 62%;
  min-width: 0;
}

.msg-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-row.mine .msg-meta {
  text-align: right;
}

.message-row.mine .bubble {
  background: #006eff;
  border-color: #006eff;
  color: #fff;
}

.empty {
  margin: auto;
}

.banned-alert {
  margin: 8px 16px 0;
  flex-shrink: 0;
}

.banned-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.appeal-remark {
  margin-top: 6px;
  font-size: 12px;
  color: #b91c1c;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 14px 24px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.input-tools {
  display: flex;
  gap: 8px;
  padding-bottom: 2px;
}

.tool-btn {
  padding: 7px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;

  &:hover:not(:disabled) {
    border-color: #93c5fd;
    color: #006eff;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.msg-image {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 280px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  text-decoration: none;
  transition: border-color 0.2s;

  &:hover {
    border-color: #93c5fd;
  }
}

.file-icon {
  padding: 4px 6px;
  border-radius: 6px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.file-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  overflow: hidden;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.member-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1f2937;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.join-requests {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.section-title {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.empty-tip {
  padding: 10px 0;
  font-size: 12px;
  color: #9ca3af;
}

.req-actions {
  display: flex;
  gap: 6px;
}

.member-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.invite-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  color: #006eff;
  font-size: 14px;
  cursor: pointer;
}

.invite-plus {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px dashed #006eff;
  color: #006eff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.invite-entry:hover .invite-plus {
  background: #eff6ff;
}

.invite-list {
  max-height: 320px;
  overflow-y: auto;
}

.invite-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  cursor: pointer;
}

.invite-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  background: #e0e7ff;
  flex-shrink: 0;
}

.invite-avatar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4338ca;
  font-weight: 600;
}

.invite-name {
  flex: 1;
  font-size: 14px;
  color: #1f2937;
}

.invite-loading {
  padding: 12px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.req-source {
  font-size: 12px;
  color: #9ca3af;
}

.dialog-tip {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
