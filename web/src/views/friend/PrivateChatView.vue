<template>
  <div class="chat-page">
    <header class="chat-header">
      <button class="back-btn" type="button" @click="goBack">‹ 返回</button>
      <div class="header-avatar">
        <img v-if="friendAvatar" :src="friendAvatar" alt="" />
        <span v-else>{{ displayName?.[0] || 'U' }}</span>
      </div>
      <div class="chat-title">
        <h2>{{ displayName }}</h2>
        <span>{{ friend.mobile || '私聊' }}</span>
      </div>
    </header>

    <div ref="messageListRef" class="message-list" @scroll="handleScroll">
      <div v-if="hasMore" class="load-more">
        <n-button text size="tiny" :loading="loadingMore" @click="loadMore">加载更早消息</n-button>
      </div>

      <div v-for="msg in messages" :key="msg.id" class="message-row" :class="{ mine: isMine(msg) }">
        <div v-if="!isMine(msg)" class="msg-avatar">
          <img v-if="friendAvatar" :src="friendAvatar" alt="" />
          <span v-else>{{ displayName?.[0] || 'U' }}</span>
        </div>
        <div class="msg-body">
          <div v-if="!isMine(msg)" class="msg-meta">{{ displayName }} · {{ formatTime(msg.createdAt) }}</div>
          <div v-else class="msg-meta right">{{ formatTime(msg.createdAt) }}</div>
          <div class="bubble">{{ msg.content }}</div>
        </div>
        <div v-if="isMine(msg)" class="msg-avatar">
          <img v-if="myAvatar" :src="myAvatar" alt="" />
          <span v-else>{{ userStore.userInfo?.username?.[0] || 'U' }}</span>
        </div>
      </div>

      <n-empty v-if="!messages.length" description="还没有聊天记录，打个招呼吧" class="empty" />
    </div>

    <footer class="chat-input">
      <n-input
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入消息，Enter 发送，Shift + Enter 换行"
        @keydown="handleKeydown"
      />
      <n-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">发送</n-button>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NInput, NEmpty, useMessage } from 'naive-ui'
import { getFriends, getPrivateMessages, sendPrivateMessage } from '@/api/friend'
import { markChatRead } from '@/api/chat'
import { resolveAvatar } from '@/utils/avatar'
import { useWebSocket } from '@/composables/useWebSocket'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const friendId = Number(route.params.friendId)

const friend = ref({})
const validFriend = ref(false)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const messageListRef = ref(null)

const { connect, disconnect } = useWebSocket()

const myId = computed(() => Number(userStore.userInfo?.id))
const displayName = computed(() => friend.value.remark || friend.value.username || '好友')
const friendAvatar = computed(() => resolveAvatar(friend.value.avatar))
const myAvatar = computed(() => resolveAvatar(userStore.userInfo?.avatar))

function isMine(msg) {
  return Number(msg.fromUserId) === myId.value
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

function goBack() {
  router.push('/chats')
}

async function loadFriend() {
  try {
    const res = await getFriends()
    friend.value = (res.data || []).find(item => Number(item.userId) === friendId) || {}
    validFriend.value = Boolean(friend.value.userId)
    if (!validFriend.value) {
      message.warning('对方还不是你的好友')
      router.replace('/friends')
    }
  } catch (e) {
    message.error(e?.message || '加载好友信息失败')
  }
}

async function loadMessages(beforeId = null) {
  try {
    const res = await getPrivateMessages(
      friendId,
      beforeId ? { beforeId, limit: 50 } : { limit: 50 }
    )
    const list = res.data || []
    if (beforeId) {
      messages.value = [...list, ...messages.value]
    } else {
      messages.value = list
    }
    hasMore.value = list.length === 50
    markRead()
  } catch (e) {
    message.error(e?.message || '加载聊天记录失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

async function loadMore() {
  if (!messages.value.length || loadingMore.value) return
  loadingMore.value = true
  const el = messageListRef.value
  const oldHeight = el ? el.scrollHeight : 0
  try {
    await loadMessages(messages.value[0].id)
    nextTick(() => {
      if (el) {
        el.scrollTop = el.scrollHeight - oldHeight
      }
    })
  } finally {
    loadingMore.value = false
  }
}

function handleScroll() {
  if (messageListRef.value && messageListRef.value.scrollTop < 40) {
    loadMore()
  }
}

function pushMessage(msg) {
  if (!msg || msg.id == null) return
  if (messages.value.some(item => item.id === msg.id)) return
  messages.value.push(msg)
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return

  sending.value = true
  try {
    const res = await sendPrivateMessage(friendId, { content })
    pushMessage(res.data)
    draft.value = ''
    scrollToBottom()
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

function markRead() {
  markChatRead({ type: 'PRIVATE', targetId: friendId }).catch(() => {})
}

function handlePrivateMessageEvent(event) {
  const detail = event.detail
  if (!detail) return
  if (Number(detail.fromUserId) !== friendId && Number(detail.toUserId) !== friendId) return
  pushMessage(detail)
  if (Number(detail.fromUserId) === friendId) {
    markRead()
  }
  scrollToBottom()
}

onMounted(async () => {
  window.addEventListener('private-message', handlePrivateMessageEvent)
  connect('')
  await loadFriend()
  if (validFriend.value) {
    await loadMessages()
    scrollToBottom()
  }
})

onUnmounted(() => {
  window.removeEventListener('private-message', handlePrivateMessageEvent)
  disconnect()
})
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
  gap: 16px;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.back-btn {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;

  &:hover {
    border-color: #93c5fd;
    color: #006eff;
  }
}

.header-avatar {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  overflow: hidden;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 16px;
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

.chat-title {
  h2 {
    margin: 0;
    font-size: 17px;
    color: #1f2937;
  }

  span {
    font-size: 12px;
    color: #9ca3af;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.load-more {
  text-align: center;
  padding-bottom: 6px;
}

.message-row {
  display: flex;
  gap: 10px;

  &.mine {
    justify-content: flex-end;
  }
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
}

.msg-meta {
  margin-bottom: 4px;
  font-size: 11px;
  color: #9ca3af;

  &.right {
    text-align: right;
  }
}

.bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-row.mine .bubble {
  background: #006eff;
  border-color: #006eff;
  color: #fff;
}

.empty {
  margin: auto;
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
</style>
