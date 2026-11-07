<template>
  <div class="home-page">
    <div class="home-header">
      <h1>欢迎使用会联</h1>
      <p>{{ currentDate }}</p>
    </div>

    <div class="home-actions">
      <div class="action-card join-card">
        <div class="action-icon join-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
        </div>
        <h3>加入会议</h3>
        <p>输入会议号加入已有会议</p>
        <div class="join-form">
          <n-input v-model:value="joinMeetingNumber" placeholder="请输入9位会议号" size="large" maxlength="9" />
          <n-input v-model:value="joinPassword" type="password" show-password-on="click" placeholder="入会密码（选填）" size="large" style="margin-top: 12px" />
          <n-button type="primary" block size="large" style="margin-top: 16px" :loading="joining" @click="handleJoin">
            加入会议
          </n-button>
        </div>
      </div>

      <div class="action-cards-right">
        <div class="action-card create-card" @click="showCreateModal = true">
          <div class="action-icon create-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
          </div>
          <h3>创建会议</h3>
          <p>立即发起一场会议</p>
        </div>

        <div class="action-card schedule-card" @click="showScheduleModal = true">
          <div class="action-icon schedule-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </div>
          <h3>预约会议</h3>
          <p>预定未来的会议</p>
        </div>

        <div class="action-card quick-card" @click="handleQuickMeeting">
          <div class="action-icon quick-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
          </div>
          <h3>快速会议</h3>
          <p>一键发起即时会议</p>
        </div>
      </div>
    </div>

    <!-- 创建会议弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" title="创建即时会议" positive-text="创建" negative-text="取消" :loading="creating" @positive-click="handleCreate">
      <n-form :model="createForm" label-placement="left" label-width="80">
        <n-form-item label="会议主题">
          <n-input v-model:value="createForm.topic" placeholder="请输入会议主题" />
        </n-form-item>
        <n-form-item label="入会密码">
          <n-input v-model:value="createForm.password" placeholder="留空则无密码" />
        </n-form-item>
        <n-form-item label="最大人数">
          <n-input-number v-model:value="createForm.maxParticipants" :min="2" :max="300" style="width: 100%" />
        </n-form-item>
        <n-form-item label="入会静音">
          <n-switch v-model:value="createForm.muteOnEntry" />
        </n-form-item>
      </n-form>
    </n-modal>

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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NInputNumber, NButton, NModal, NForm, NFormItem, NSwitch, NDatePicker, useMessage } from 'naive-ui'
import { useMeetingStore } from '@/store/meeting'
import dayjs from 'dayjs'

const router = useRouter()
const meetingStore = useMeetingStore()
const message = useMessage()

const currentDate = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))

const joinMeetingNumber = ref('')
const joinPassword = ref('')
const joining = ref(false)

const showCreateModal = ref(false)
const creating = ref(false)
const createForm = ref({
  topic: '即时会议',
  password: '',
  maxParticipants: 300,
  muteOnEntry: false
})

const showScheduleModal = ref(false)
const scheduling = ref(false)
const scheduleForm = ref({
  topic: '预约会议',
  scheduledStartTime: Date.now() + 3600000,
  duration: 60,
  password: ''
})

async function handleJoin() {
  if (!joinMeetingNumber.value || joinMeetingNumber.value.length !== 9) {
    message.warning('请输入9位会议号')
    return
  }
  joining.value = true
  try {
    const result = await meetingStore.joinMeetingByNumber(joinMeetingNumber.value, joinPassword.value)
    router.push(`/meeting/${result.meeting.id}`)
  } catch (e) {
    message.error(e.message || e.response?.data?.message || '加入会议失败')
  } finally {
    joining.value = false
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

async function handleCreate() {
  creating.value = true
  try {
    const meeting = await meetingStore.createInstant(normalizePassword(createForm.value))
    router.push(`/meeting/${meeting.id}`)
  } catch (e) {
    message.error(e.message || e.response?.data?.message || '创建会议失败')
    throw e
  } finally {
    creating.value = false
  }
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
  } catch (e) {
    message.error(e.message || e.response?.data?.message || '预约失败')
    throw e
  } finally {
    scheduling.value = false
  }
}

async function handleQuickMeeting() {
  try {
    const meeting = await meetingStore.createInstant({ topic: '快速会议', maxParticipants: 300 })
    router.push(`/meeting/${meeting.id}`)
  } catch (e) {
    message.error('创建会议失败')
  }
}
</script>

<style scoped lang="scss">
.home-page {
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.home-header {
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0 0 8px;
  }

  p {
    color: #999;
    margin: 0;
    font-size: 14px;
  }
}

.home-actions {
  display: flex;
  gap: 20px;
  margin-bottom: 40px;
}

.action-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #eee;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 12px 0 6px;
  }

  p {
    color: #999;
    font-size: 13px;
    margin: 0;
  }
}

.join-card {
  flex: 1;
  min-width: 320px;
}

.action-cards-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

.action-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.join-icon {
  background: linear-gradient(135deg, #006EFF, #4098FC);
  color: #fff;
}

.create-icon {
  background: linear-gradient(135deg, #36D399, #22C55E);
  color: #fff;
}

.schedule-icon {
  background: linear-gradient(135deg, #F59E0B, #F97316);
  color: #fff;
}

.quick-icon {
  background: linear-gradient(135deg, #8B5CF6, #A855F7);
  color: #fff;
}

.join-form {
  margin-top: 20px;
}

.action-card.create-card,
.action-card.schedule-card,
.action-card.quick-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;

  h3, p {
    margin: 0;
  }

  h3 {
    font-size: 16px;
  }

  p {
    font-size: 12px;
    margin-top: 4px !important;
  }
}
</style>
