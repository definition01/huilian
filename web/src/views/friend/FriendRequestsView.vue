<template>
  <div class="requests-page">
    <div class="page-header">
      <button class="back-btn" type="button" @click="router.push('/contacts')">‹ 返回</button>
      <div>
        <h1>新的朋友</h1>
        <p>处理好友申请，等待对方验证</p>
      </div>
      <n-button size="small" :loading="loading" @click="loadData">刷新</n-button>
    </div>

    <n-spin :show="loading">
      <div class="section-card">
        <div class="section-title">收到的申请（{{ received.length }}）</div>
        <div v-if="received.length">
          <div v-for="req in received" :key="req.id" class="request-row">
            <img v-if="req.avatar" :src="resolveAvatar(req.avatar)" class="avatar-img" alt="" />
            <div v-else class="avatar received-avatar">{{ req.username?.[0] || 'U' }}</div>
            <div class="info">
              <div class="name">{{ req.username }}</div>
              <div class="sub">{{ req.applyMessage || '请求添加你为好友' }}</div>
            </div>
            <n-button size="small" type="primary" :loading="reviewingId === req.id" @click="handleReview(req, true)">同意</n-button>
            <n-button size="small" quaternary type="error" :loading="reviewingId === req.id" @click="handleReview(req, false)">拒绝</n-button>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="暂无待处理的好友申请" class="empty" />
      </div>

      <div class="section-card">
        <div class="section-title">我发出的申请（{{ sent.length }}）</div>
        <div v-if="sent.length">
          <div v-for="req in sent" :key="req.id" class="request-row">
            <div class="avatar sent-avatar">{{ req.toUsername?.[0] || 'U' }}</div>
            <div class="info">
              <div class="name">{{ req.toUsername || '未知用户' }}</div>
              <div class="sub">{{ req.applyMessage || '请求添加对方为好友' }}</div>
            </div>
            <n-tag size="small" type="warning">等待验证</n-tag>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="暂无发出的好友申请" class="empty" />
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, NSpin, NEmpty, useMessage } from 'naive-ui'
import {
  getReceivedFriendRequests,
  getSentFriendRequests,
  approveFriendRequest,
  rejectFriendRequest
} from '@/api/friend'
import { resolveAvatar } from '@/utils/avatar'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const received = ref([])
const sent = ref([])
const reviewingId = ref(null)

async function loadData() {
  loading.value = true
  try {
    const [receivedRes, sentRes] = await Promise.all([
      getReceivedFriendRequests(),
      getSentFriendRequests()
    ])
    received.value = receivedRes.data || []
    sent.value = sentRes.data || []
  } catch (e) {
    message.error(e?.message || '加载好友申请失败')
  } finally {
    loading.value = false
  }
}

async function handleReview(req, approve) {
  if (reviewingId.value) return
  reviewingId.value = req.id
  try {
    if (approve) {
      await approveFriendRequest(req.id)
      message.success('已添加为好友')
    } else {
      await rejectFriendRequest(req.id)
      message.success('已拒绝该申请')
    }
    await loadData()
  } catch (e) {
    message.error(e?.message || '操作失败')
  } finally {
    reviewingId.value = null
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.requests-page {
  padding: 32px;
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h1 {
    margin: 0;
    font-size: 22px;
    color: #1f2937;
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: #6b7280;
  }

  .back-btn {
    padding: 7px 12px;
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

  > div {
    flex: 1;
  }
}

.section-card {
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.section-title {
  padding: 14px 18px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #f3f4f6;
}

.request-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #f1f5f9;
  flex-shrink: 0;
}

.received-avatar {
  background: #006eff;
}

.sent-avatar {
  background: #94a3b8;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 15px;
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

.empty {
  padding: 24px 0;
}
</style>
