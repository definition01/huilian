<template>
  <div class="chats-page">
    <div class="page-header">
      <div>
        <h1>聊天</h1>
        <p>群聊与私聊会话都在这里</p>
      </div>
      <n-button size="small" :loading="loading" @click="loadConversations">刷新</n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="conversations.length" class="chat-card">
        <div
          v-for="conv in conversations"
          :key="conv.key"
          :class="['chat-row', { pinned: conv.pinned }]"
          @click="openChat(conv)"
        >
          <div class="avatar-wrap">
            <img v-if="conv.avatar" :src="resolveAvatar(conv.avatar)" class="avatar-img" alt="" />
            <div v-else class="avatar" :style="{ background: avatarColor(conv.name) }">
              {{ conv.avatarText }}
            </div>
            <span v-if="conv.unread > 0" class="unread-badge">
              {{ conv.unread > 99 ? '99+' : conv.unread }}
            </span>
          </div>
          <div class="row-main">
            <div class="row-top">
              <span class="name">
                {{ conv.name }}
                <span v-if="conv.type === 'group'" class="group-badge">群</span>
              </span>
              <div class="row-actions">
                <button
                  class="pin-btn"
                  type="button"
                  :title="conv.pinned ? '取消置顶' : '置顶'"
                  @click.stop="togglePin(conv)"
                >
                  {{ conv.pinned ? '取消置顶' : '置顶' }}
                </button>
                <span class="time">{{ formatTime(conv.time) }}</span>
              </div>
            </div>
            <div class="row-bottom">
              <span class="last">{{ conv.lastMessage || conv.sub }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="chat-card empty-card">
        <n-empty description="还没有聊天，去通讯录找好友或群聊吧">
          <template #extra>
            <n-button size="small" type="primary" ghost @click="router.push('/contacts')">去通讯录</n-button>
          </template>
        </n-empty>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NSpin, NEmpty, useMessage } from 'naive-ui'
import { getMyGroups } from '@/api/group'
import { getFriends } from '@/api/friend'
import { pinChat } from '@/api/chat'
import { resolveAvatar } from '@/utils/avatar'

const router = useRouter()
const message = useMessage()

const groups = ref([])
const friends = ref([])
const loading = ref(false)

let reloadTimer = null

const conversations = computed(() => {
  const list = [
    ...groups.value.map(group => ({
      key: `group-${group.id}`,
      type: 'group',
      targetId: group.id,
      name: group.groupName,
      avatarText: group.groupName?.[0] || 'G',
      lastMessage: group.lastMessage,
      time: group.lastMessageTime,
      sub: `${group.memberCount || 0} 名成员`,
      unread: Number(group.unreadCount) || 0,
      pinned: Boolean(group.pinned)
    })),
    ...friends.value.map(friend => ({
      key: `friend-${friend.userId}`,
      type: 'friend',
      targetId: friend.userId,
      name: friend.remark || friend.username,
      avatar: friend.avatar,
      avatarText: (friend.remark || friend.username)?.[0] || 'U',
      lastMessage: friend.lastMessage,
      time: friend.lastMessageTime,
      sub: friend.username,
      unread: Number(friend.unreadCount) || 0,
      pinned: Boolean(friend.pinned)
    }))
  ]

  return list.sort((a, b) => {
    // 置顶会话优先展示，其次按最后消息时间倒序
    if (a.pinned !== b.pinned) {
      return a.pinned ? -1 : 1
    }
    const timeA = a.time ? new Date(String(a.time).replace(' ', 'T')).getTime() : 0
    const timeB = b.time ? new Date(String(b.time).replace(' ', 'T')).getTime() : 0
    return timeB - timeA
  })
})

function avatarColor(name) {
  const colors = ['#006eff', '#12b7f5', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#0ea5e9', '#6366f1']
  let hash = 0
  for (let i = 0; i < (name || '').length; i++) {
    hash = (hash + name.charCodeAt(i)) % colors.length
  }
  return colors[hash]
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }

  const diffDays = Math.floor((now - date) / (24 * 60 * 60 * 1000))
  if (diffDays < 7) {
    return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  }
  return `${date.getMonth() + 1}-${date.getDate()}`
}

async function loadConversations() {
  loading.value = true
  try {
    const [groupRes, friendRes] = await Promise.all([getMyGroups(), getFriends()])
    groups.value = groupRes.data || []
    friends.value = friendRes.data || []
  } finally {
    loading.value = false
  }
}

function openChat(conv) {
  if (conv.type === 'group') {
    router.push(`/groups/${conv.targetId}`)
  } else {
    router.push(`/friends/${conv.targetId}`)
  }
}

async function togglePin(conv) {
  try {
    await pinChat({
      type: conv.type === 'group' ? 'GROUP' : 'PRIVATE',
      targetId: conv.targetId,
      pinned: !conv.pinned
    })
    await loadConversations()
  } catch (e) {
    message.error(e?.message || '设置置顶失败')
  }
}

function scheduleReload() {
  if (reloadTimer) clearTimeout(reloadTimer)
  reloadTimer = setTimeout(() => {
    loadConversations()
  }, 600)
}

onMounted(() => {
  loadConversations()
  window.addEventListener('group-message', scheduleReload)
  window.addEventListener('private-message', scheduleReload)
})

onUnmounted(() => {
  window.removeEventListener('group-message', scheduleReload)
  window.removeEventListener('private-message', scheduleReload)
  if (reloadTimer) clearTimeout(reloadTimer)
})
</script>

<style scoped lang="scss">
.chats-page {
  padding: 32px;
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.chat-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.empty-card {
  padding: 40px 0;
}

.chat-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
  }

  &.pinned {
    background: #f5f7fa;

    &:hover {
      background: #f1f5f9;
    }
  }
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar,
.avatar-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: block;
}

.avatar {
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.avatar-img {
  object-fit: cover;
  background: #f1f5f9;
}

.unread-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  box-sizing: border-box;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-badge {
  display: inline-block;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 4px;
  background: #eef4ff;
  color: #006eff;
  font-size: 11px;
  font-weight: 400;
  vertical-align: middle;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.pin-btn {
  padding: 2px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, border-color 0.15s;

  &:hover {
    border-color: #93c5fd;
    color: #006eff;
  }
}

.chat-row:hover .pin-btn {
  opacity: 1;
}

.chat-row.pinned .pin-btn {
  opacity: 1;
  color: #006eff;
  border-color: #dbeafe;
  background: #eef4ff;
}

.time {
  font-size: 12px;
  color: #b0b5bd;
  flex-shrink: 0;
}

.row-bottom {
  margin-top: 5px;
}

.last {
  display: block;
  font-size: 12.5px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
