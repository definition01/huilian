<template>
  <div class="meeting-room" :class="{ 'has-sidebar': !!showSidebar }">
    <!-- 顶部栏 -->
    <div class="meeting-topbar">
      <div class="topbar-left">
        <span
          class="live-dot"
          :class="{ 'is-idle': meetingStore.currentMeeting?.status !== 'ongoing' }"
        ></span>
        <span class="meeting-topic">{{ meetingStore.currentMeeting?.topic || '会议中' }}</span>
        <span class="meeting-number">会议号 {{ meetingStore.currentMeeting?.meetingNumber }}</span>
      </div>
      <div class="topbar-center">
        <span class="meeting-timer" v-if="meetingStore.currentMeeting?.status === 'ongoing'">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          {{ formatDuration(meetingDuration) }}
        </span>
      </div>
      <div class="topbar-right">
        <n-button quaternary size="small" @click="showInfo = !showInfo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
        </n-button>
      </div>
    </div>

    <!-- 视频区域 -->
    <div class="video-area">
      <div class="video-grid" :class="{ 'has-sidebar': showSidebar }">
        <!-- 远程视频 -->
        <div v-for="[userId, stream] in remoteStreams" :key="userId" class="video-cell remote-video">
          <video :ref="el => bindRemoteVideo(userId, el)" autoplay playsinline @click="unmuteRemote"></video>
          <div class="video-label">{{ getParticipantName(userId) }}</div>
        </div>

        <!-- 已加入但还没有视频的参与者 -->
        <div
          v-for="p in waitingParticipants"
          :key="'waiting-' + p.userId"
          class="video-cell participant-tile"
        >
          <div class="participant-avatar-lg">{{ p.username?.[0] || 'U' }}</div>
          <div class="video-label">{{ p.username }}</div>
        </div>

        <!-- 本地视频 -->
        <div class="video-cell local-video" v-if="webrtc.isCameraOn.value">
          <video ref="localVideoRef" autoplay playsinline muted></video>
          <div class="video-label">我</div>
        </div>

        <!-- 无视频时的占位 -->
        <div
          v-if="!webrtc.isCameraOn.value && remoteStreams.size === 0 && waitingParticipants.length === 0"
          class="no-video-placeholder"
        >
          <div class="placeholder-avatar">{{ userStore?.userInfo?.username?.[0] || 'U' }}</div>
        </div>
      </div>

      <!-- 实时字幕 -->
      <div v-if="showSubtitles && currentSubtitle" class="subtitle-overlay">
        <div class="subtitle-box">
          <div class="subtitle-line">
            <span class="subtitle-speaker">{{ currentSubtitle.userName }}：</span>
            <span class="subtitle-text">{{ currentSubtitle.text }}</span>
          </div>
          <div v-if="currentTranslation" class="subtitle-translation">
            <span class="subtitle-translation-lang">{{ languageLabel(currentTranslation.targetLanguage) }}</span>
            <span>{{ currentTranslation.translatedText }}</span>
          </div>
        </div>
      </div>

      <!-- 聊天面板 -->
      <div class="chat-panel" v-if="showSidebar === 'chat'">
        <div class="chat-header">
          <span>会议聊天</span>
          <n-button quaternary size="tiny" @click="showSidebar = null">✕</n-button>
        </div>
        <div class="chat-messages" ref="chatMessagesRef">
          <div v-for="msg in meetingStore.chatMessages" :key="msg.id" class="chat-message" :class="{ 'is-system': msg.type === 'SYSTEM' }">
            <template v-if="msg.type === 'SYSTEM'">
              <span class="system-text">{{ msg.content }}</span>
            </template>
            <template v-else>
              <div class="msg-header">
                <span class="msg-sender">{{ msg.senderName }}</span>
                <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <div class="msg-content">{{ msg.content }}</div>
            </template>
          </div>
        </div>
        <div class="chat-input">
          <n-input v-model:value="chatInput" placeholder="输入消息..." @keyup.enter="sendChatMessage" />
          <n-button type="primary" @click="sendChatMessage" :disabled="!chatInput.trim()">发送</n-button>
        </div>
      </div>

      <!-- 参会人面板 -->
      <div class="participants-panel" v-if="showSidebar === 'participants'">
        <div class="panel-header">
          <span>参会人 ({{ meetingStore.participants.length }})</span>
          <div class="header-actions">
            <n-button v-if="isHost" quaternary size="tiny" @click="muteAllParticipants" title="全体静音">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="1" y1="1" x2="23" y2="23"/>
                <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/>
                <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2c0 .76-.12 1.49-.34 2.17"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
            </n-button>
            <n-button quaternary size="tiny" @click="showSidebar = null">✕</n-button>
          </div>
        </div>
        <div class="participant-list">
          <div v-for="p in meetingStore.participants" :key="p.userId" class="participant-item">
            <div class="participant-avatar">{{ p.username?.[0] || 'U' }}</div>
            <div class="participant-info">
              <span class="participant-name">
                {{ p.username }}
                <span v-if="p.role === 'host'" class="host-badge">主持人</span>
              </span>
              <span class="participant-role">{{ getRoleText(p.role) }}</span>
            </div>
            <div class="participant-status">
              <span v-if="p.isMuted" class="status-icon mic-off">🔇</span>
              <span v-else class="status-icon mic-on">🎤</span>

              <!-- 主持人控制按钮 -->
              <div v-if="isHost && p.userId !== userStore?.userInfo?.id" class="host-controls">
                <n-button
                  quaternary
                  size="tiny"
                  @click="muteParticipant(p.userId, !p.isMuted)"
                  :title="p.isMuted ? '取消静音' : '静音该参与者'"
                >
                  <svg v-if="!p.isMuted" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="1" y1="1" x2="23" y2="23"/>
                    <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/>
                    <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2c0 .76-.12 1.49-.34 2.17"/>
                    <line x1="12" y1="19" x2="12" y2="23"/>
                    <line x1="8" y1="23" x2="16" y2="23"/>
                  </svg>
                  <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                    <line x1="12" y1="19" x2="12" y2="23"/>
                    <line x1="8" y1="23" x2="16" y2="23"/>
                  </svg>
                </n-button>
                <n-button
                  quaternary
                  size="tiny"
                  type="error"
                  @click="removeParticipant(p.userId, p.username)"
                  title="移除该参与者"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </n-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 助手面板 -->
      <div class="ai-panel" v-if="showSidebar === 'ai'">
        <div class="panel-header">
          <span>AI 助手</span>
          <n-button quaternary size="tiny" @click="showSidebar = null">✕</n-button>
        </div>
        <div class="ai-body">
          <section class="ai-section">
            <div class="ai-section-title">实时翻译</div>
            <div class="ai-row">
              <span class="ai-row-label">翻译开关</span>
              <n-switch
                v-model:value="aiSettings.translationEnabled"
                :disabled="!isHost || aiSettingsSaving"
                @update:value="saveAiSettings"
              />
            </div>
            <div class="ai-row">
              <span class="ai-row-label">目标语言</span>
              <n-select
                v-model:value="aiSettings.targetLanguages"
                :options="languageOptions"
                multiple
                size="small"
                style="width: 160px"
                :disabled="!isHost || aiSettingsSaving"
                @update:value="saveAiSettings"
              />
            </div>
            <p v-if="!isHost" class="ai-tip">仅主持人可以修改 AI 配置</p>
          </section>

          <section class="ai-section">
            <div class="ai-section-title">
              <span>翻译记录</span>
              <span class="ai-count">{{ translationRecords.length }}</span>
            </div>
            <div v-if="translationRecords.length === 0" class="ai-empty">
              开启实时翻译后，译文会显示在这里
            </div>
            <div v-for="item in translationRecords" :key="item.key" class="translation-item">
              <div class="translation-meta">
                <span class="translation-speaker">{{ item.userName || '参会人' }}</span>
                <span class="translation-lang">{{ languageLabel(item.targetLanguage) }}</span>
              </div>
              <div class="translation-source">{{ item.sourceText }}</div>
              <div class="translation-text">{{ item.translatedText }}</div>
            </div>
          </section>

          <section class="ai-section">
            <div class="ai-section-title">
              <span>AI 会议纪要</span>
              <n-button
                v-if="isHost"
                size="tiny"
                type="primary"
                :loading="summaryGenerating"
                @click="triggerAiSummary"
              >
                {{ aiSummary?.status === 'success' ? '重新生成' : '生成纪要' }}
              </n-button>
            </div>

            <template v-if="aiSummary && aiSummary.status === 'success'">
              <div class="summary-title">{{ aiSummary.title || '会议纪要' }}</div>
              <p class="summary-overview">{{ aiSummary.overview }}</p>

              <div v-if="aiSummary.keyPoints?.length" class="summary-block">
                <div class="summary-block-title">关键要点</div>
                <ul>
                  <li v-for="(point, idx) in aiSummary.keyPoints" :key="'kp-' + idx">{{ point }}</li>
                </ul>
              </div>

              <div v-if="aiSummary.decisions?.length" class="summary-block">
                <div class="summary-block-title">决议事项</div>
                <ul>
                  <li v-for="(item, idx) in aiSummary.decisions" :key="'dc-' + idx">{{ item }}</li>
                </ul>
              </div>

              <div v-if="aiSummary.actionItems?.length" class="summary-block">
                <div class="summary-block-title">待办事项</div>
                <ul>
                  <li v-for="(item, idx) in aiSummary.actionItems" :key="'ac-' + idx">
                    {{ item.task }}
                    <span v-if="item.owner" class="action-owner">@{{ item.owner }}</span>
                    <span v-if="item.dueDate" class="action-due">{{ item.dueDate }}</span>
                  </li>
                </ul>
              </div>

              <div v-if="aiSummary.keywords?.length" class="summary-keywords">
                <n-tag v-for="(kw, idx) in aiSummary.keywords" :key="'kw-' + idx" size="tiny" round :bordered="false">
                  {{ kw }}
                </n-tag>
              </div>
            </template>

            <div v-else-if="aiSummary && aiSummary.status === 'failed'" class="ai-empty">
              {{ aiSummary.errorMessage || '纪要生成失败' }}
            </div>
            <div v-else-if="summaryGenerating" class="ai-empty">AI 正在整理会议内容，请稍候…</div>
            <div v-else class="ai-empty">会议结束后会自动生成，主持人也可手动触发</div>
          </section>
        </div>
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div class="meeting-toolbar">
      <div class="toolbar-left">
        <div class="toolbar-item">
          <n-button quaternary circle size="large" @click="toggleMic" :type="webrtc.isMicOn.value ? 'primary' : 'error'">
            <svg v-if="webrtc.isMicOn.value" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="1" y1="1" x2="23" y2="23"/>
              <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/>
              <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2c0 .76-.12 1.49-.34 2.17"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </n-button>
          <span class="toolbar-label">{{ webrtc.isMicOn.value ? '麦克风' : '麦克风已关闭' }}</span>
        </div>

        <div class="toolbar-item">
          <n-button quaternary circle size="large" @click="toggleCamera" :type="webrtc.isCameraOn.value ? 'primary' : 'error'">
            <svg v-if="webrtc.isCameraOn.value" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="23 7 16 12 23 17 23 7"/>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16.5 9.4l.94-2.06.76-1.73M1 1l22 22M21 21H3a2 2 0 01-2-2V8a2 2 0 012-2h3m3-3h6l2 3h4a2 2 0 012 2v9.34m-7.72-2.06a4 4 0 11-7.18-1.46"/>
            </svg>
          </n-button>
          <span class="toolbar-label">{{ webrtc.isCameraOn.value ? '摄像头' : '摄像头已关闭' }}</span>
        </div>

        <div class="toolbar-item">
          <n-button quaternary circle size="large" @click="toggleScreenShare" :type="webrtc.isScreenSharing.value ? 'warning' : 'default'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
              <line x1="8" y1="21" x2="16" y2="21"/>
              <line x1="12" y1="17" x2="12" y2="21"/>
            </svg>
          </n-button>
          <span class="toolbar-label">{{ webrtc.isScreenSharing.value ? '停止共享' : '共享屏幕' }}</span>
        </div>

        <div class="toolbar-item">
          <n-button quaternary circle size="large" @click="toggleSubtitles" :type="showSubtitles ? 'primary' : 'default'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="4" width="20" height="16" rx="2"/>
              <line x1="6" y1="15" x2="10" y2="15"/>
              <line x1="14" y1="15" x2="18" y2="15"/>
            </svg>
          </n-button>
          <span class="toolbar-label">{{ showSubtitles ? '关闭字幕' : '开启字幕' }}</span>
        </div>
      </div>

      <div class="toolbar-center">
        <n-button v-if="isHost && meetingStore.currentMeeting?.status === 'waiting'" type="primary" size="large" @click="handleStartMeeting">
          开始会议
        </n-button>
        <n-button v-if="isHost && meetingStore.currentMeeting?.status === 'ongoing'" type="error" size="large" @click="handleEndMeeting">
          结束会议
        </n-button>
      </div>

      <div class="toolbar-right">
        <n-button quaternary circle size="large" @click="toggleSidebar('ai')" :type="showSidebar === 'ai' ? 'primary' : 'default'" title="AI 助手">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 3l1.9 4.6L18.5 9.5l-4.6 1.9L12 16l-1.9-4.6L5.5 9.5l4.6-1.9L12 3z"/>
            <path d="M19 15l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9.9-2.1z"/>
          </svg>
        </n-button>

        <n-button quaternary circle size="large" @click="toggleSidebar('chat')" :type="showSidebar === 'chat' ? 'primary' : 'default'">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
          </svg>
        </n-button>

        <n-button quaternary circle size="large" @click="toggleSidebar('participants')" :type="showSidebar === 'participants' ? 'primary' : 'default'">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </n-button>

        <n-button quaternary circle size="large" type="error" @click="handleLeave">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NInput, NSelect, NSwitch, NTag, useMessage, useDialog } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { useMeetingStore } from '@/store/meeting'
import { useWebSocket } from '@/composables/useWebSocket'
import { useWebRTC } from '@/composables/useWebRTC'
import { getParticipants, removeParticipant as removeParticipantApi, muteParticipant as muteParticipantApi } from '@/api/meeting'
import { startAsr, stopAsr } from '@/api/transcript'
import { getAiSettings, updateAiSettings, getTranslations, getMeetingSummary, triggerMeetingSummary } from '@/api/ai'
import { AudioCapture } from '@/utils/audioCapture'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const meetingStore = useMeetingStore()
const message = useMessage()
const dialog = useDialog()

const ws = useWebSocket()
const webrtc = useWebRTC()

const meetingId = computed(() => route.params.meetingId)
const meetingDuration = ref(0)
const timer = ref(null)
let participantTimer = null
const showSidebar = ref(null)
const showInfo = ref(false)
const chatInput = ref('')
const chatMessagesRef = ref(null)

// ASR字幕相关
const showSubtitles = ref(false)
const currentSubtitle = ref(null)
const subtitleTimer = ref(null)
const audioCapture = ref(null)
const localVideoRef = ref(null)

// AI 实时翻译与会议纪要
const languageOptions = [
  { label: '中文', value: 'zh' },
  { label: '英语', value: 'en' },
  { label: '日语', value: 'ja' },
  { label: '韩语', value: 'ko' },
  { label: '法语', value: 'fr' },
  { label: '德语', value: 'de' },
  { label: '西班牙语', value: 'es' },
  { label: '俄语', value: 'ru' },
  { label: '葡萄牙语', value: 'pt' }
]
const aiSettings = ref({
  translationEnabled: false,
  sourceLanguage: 'auto',
  targetLanguages: ['en'],
  summaryEnabled: true,
  summaryLanguage: 'zh',
  autoSummary: true
})
const aiSettingsSaving = ref(false)
const translationRecords = ref([])
const aiSummary = ref(null)
const currentTranslation = ref(null)
const translationTimer = ref(null)
const summaryPollTimer = ref(null)
let summaryPollAttempts = 0
const summaryGenerating = computed(() =>
  ['pending', 'generating'].includes(aiSummary.value?.status)
)

const isHost = computed(() => {
  return meetingStore.currentMeeting?.hostUserId === userStore?.userInfo?.id
})

onMounted(async () => {
  try {
    // 直接刷新会议页时（不经过 MainLayout）需要自己加载用户信息，
    // 否则 userInfo 为空，isHost、主持人控制、ASR 等依赖用户 ID 的功能都会失效
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }

    // 协商发起方需要在 glare 时判定 polite 角色
    webrtc.setLocalUserId(userStore.userInfo?.id)

    // 清空上一场会议残留的聊天记录
    meetingStore.clearChat()

    // 事件监听必须在建立 WebSocket 之前注册，否则握手期间到达的 OFFER/ANSWER 会被丢弃
    window.addEventListener('meeting-control', handleControlEvent)
    window.addEventListener('meeting-signaling', handleSignalingEvent)
    window.addEventListener('meeting-subtitle', handleSubtitleEvent)
    window.addEventListener('meeting-translation', handleTranslationEvent)
    window.addEventListener('meeting-summary', handleSummaryEvent)

    // 先连 WebSocket 再建立连接，否则首轮的 offer/answer 会因为连接未就绪被静默丢弃
    ws.connect(meetingId.value, async () => {
      // 广播“加入会议”信令，其他人据此刷新参会人列表并建立连接
      ws.sendSignaling(JSON.stringify({ type: 'join-room' }), meetingId.value)
      await loadParticipants()
    })

    await meetingStore.fetchMeetingDetail(meetingId.value)
    await loadParticipants()
    await loadAiSettings()
    await loadSummary()
    if (aiSummary.value && ['pending', 'generating'].includes(aiSummary.value.status)) {
      startSummaryPoll()
    }

    startTimer()

    // 定时刷新参会人，兜底处理漏掉的加入/离开通知，并发现新参与者后自动建立连接
    participantTimer = setInterval(() => {
      loadParticipants()
    }, 5000)

  } catch (e) {
    console.error('加载会议失败:', e)
    message.error(e.response?.data?.message || '获取会议信息失败')
    // 如果是401错误，会自动跳转到登录页
    if (e.response?.status !== 401) {
      // 其他错误则返回首页
      setTimeout(() => router.push('/home'), 2000)
    }
  }
})

onUnmounted(() => {
  ws.disconnect()
  webrtc.cleanup()
  stopTimer()

  if (participantTimer) {
    clearInterval(participantTimer)
    participantTimer = null
  }

  lateOfferTimers.forEach(timer => clearTimeout(timer))
  lateOfferTimers.clear()

  remoteVideoEls.clear()
  window.removeEventListener('meeting-control', handleControlEvent)
  window.removeEventListener('meeting-signaling', handleSignalingEvent)
  window.removeEventListener('meeting-subtitle', handleSubtitleEvent)
  window.removeEventListener('meeting-translation', handleTranslationEvent)
  window.removeEventListener('meeting-summary', handleSummaryEvent)

  // 清理字幕定时器
  if (subtitleTimer.value) {
    clearTimeout(subtitleTimer.value)
  }
  if (translationTimer.value) {
    clearTimeout(translationTimer.value)
  }
  clearSummaryPoll()

  // 清理音频采集
  if (audioCapture.value) {
    audioCapture.value.stop()
    audioCapture.value = null
  }
})

watch(() => webrtc.localStream.value, (stream) => {
  if (stream && localVideoRef.value) {
    localVideoRef.value.srcObject = stream
  }
}, { immediate: true })

const remoteStreams = computed(() => webrtc.remoteStreams.value)

// 除自己以外、还没有收到视频流的参会者，用头像占位显示
const waitingParticipants = computed(() => {
  const myId = String(userStore?.userInfo?.id ?? '')
  return meetingStore.participants.filter(p => {
    const userId = String(p.userId)
    return userId !== myId && !webrtc.remoteStreams.value.has(userId)
  })
})

/**
 * 拉取参会人并建立连接
 */
// 延迟发起 offer 的定时器（userId 较大的一方使用，避免双方同时 offer 互相丢弃）
const lateOfferTimers = new Map()

function scheduleLateOffer(userId, sendSignal, delay = 1200) {
  if (lateOfferTimers.has(userId)) return

  const timer = setTimeout(() => {
    lateOfferTimers.delete(userId)
    const pc = webrtc.peerConnections.value.get(userId)
    // 已经收到对方的 offer 说明协商已被对方接手，不需要自己再发起
    if (!pc || pc.remoteDescription) return
    webrtc.createOffer(userId, sendSignal)
  }, delay)

  lateOfferTimers.set(userId, timer)
}

async function loadParticipants() {
  try {
    // 先把卡在“没收到远端描述”的僵尸连接清掉，下一轮会重建
    webrtc.cleanupStaleConnections()

    const res = await getParticipants(meetingId.value)
    meetingStore.updateParticipants(res.data)

    const currentUserId = Number(userStore?.userInfo?.id)
    if (!Number.isFinite(currentUserId)) return

    // WebSocket 还没连上时不要发 offer，否则本地会进入 have-local-offer 状态却永远收不到 answer
    if (!ws.connected.value) return

    res.data.forEach(participant => {
      const userId = participant.userId.toString()
      // 不要给自己发送offer
      if (userId === String(currentUserId)) return

      const sendSignal = (content, targetUserId) => {
        ws.sendSignaling(content, meetingId.value, targetUserId)
      }

      webrtc.createPeerConnection(userId, sendSignal)

      if (currentUserId < Number(participant.userId)) {
        // userId 较小的一方立即发起，抢占协商主动权
        webrtc.createOffer(userId, sendSignal)
      } else {
        // userId 较大的一方延迟发起：如果对方已经先发 offer 并完成应答，
        // 就不再发 offer，避免双方同时处于 have-local-offer 互相丢弃
        scheduleLateOffer(userId, sendSignal)
      }
    })
  } catch (e) {
    console.error(e)
  }
}

// 本地摄像头/麦克风变化后同步轨道，并让每条已建立的连接重新协商，
// 这样新增的轨道（比如后开的摄像头）才能被对方收到
async function refreshPeerConnections() {
  await webrtc.syncLocalTracksToPeers()

  for (const userId of Array.from(webrtc.peerConnections.value.keys())) {
    await webrtc.renegotiate(userId, (content, targetUserId) => {
      ws.sendSignaling(content, meetingId.value, targetUserId)
    })
  }
}

// 麦克风静音时停止向ASR上报音频，避免“麦克风已关闭仍产生字幕”
function syncAsrCaptureWithMic() {
  audioCapture.value?.setEnabled(webrtc.isMicOn.value)
}

async function toggleMic() {
  try {
    if (!webrtc.isMicOn.value && !webrtc.localStream.value) {
      await webrtc.startCamera()
      // 开启麦克风后重建连接，确保对方能收到新轨道
      await refreshPeerConnections()
    } else {
      webrtc.toggleMic()
    }
    ws.sendControl(webrtc.isMicOn.value ? 'UNMUTE' : 'MUTE', meetingId.value)
    syncAsrCaptureWithMic()

    // 持久化自己的麦克风状态，否则其他人刷新参会人列表时看到的还是旧状态
    const currentUserId = userStore.userInfo?.id
    if (currentUserId != null) {
      try {
        await muteParticipantApi(meetingId.value, currentUserId, !webrtc.isMicOn.value)
        await loadParticipants()
      } catch (e) {
        console.warn('同步麦克风状态失败', e)
      }
    }
  } catch (e) {
    message.error(e.message || '无法打开麦克风')
  }
}

async function toggleCamera() {
  try {
    if (!webrtc.isCameraOn.value && !webrtc.localStream.value) {
      await webrtc.startCamera()
    } else {
      webrtc.toggleCamera()
    }
    await nextTick()
    if (localVideoRef.value && webrtc.localStream.value) {
      localVideoRef.value.srcObject = webrtc.localStream.value
    }
    // 摄像头每次开关都重新协商：只切换 track.enabled 时对方收到的仍是 recvonly，
    // 必须先 updateTrack 再 offer，对方的 SDP 才会更新成能接收视频
    await refreshPeerConnections()
    ws.sendControl(webrtc.isCameraOn.value ? 'CAMERA_ON' : 'CAMERA_OFF', meetingId.value)
  } catch (e) {
    message.error(e.message || '无法打开摄像头')
  }
}

async function toggleScreenShare() {
  if (webrtc.isScreenSharing.value) {
    webrtc.stopScreenShare()
    ws.sendControl('SCREEN_SHARE_STOP', meetingId.value)
  } else {
    await webrtc.startScreenShare()
    ws.sendControl('SCREEN_SHARE_START', meetingId.value)
  }
}

function sendChatMessage() {
  if (!chatInput.value.trim()) return
  // 只发送到 WebSocket，不立即添加到本地
  // 等后端广播回来后再显示（通过 handleMessage 处理）
  ws.sendText(chatInput.value, meetingId.value)
  chatInput.value = ''
}

function toggleSidebar(type) {
  showSidebar.value = showSidebar.value === type ? null : type
}

async function handleStartMeeting() {
  try {
    await meetingStore.startCurrentMeeting(meetingId.value)
    message.success('会议已开始')
    // 会议真正开始时才以 actualStartTime 为基准重新计时
    startTimer()
    try {
      await webrtc.startCamera()
      await nextTick()
      if (localVideoRef.value && webrtc.localStream.value) {
        localVideoRef.value.srcObject = webrtc.localStream.value
      }
      // 已经提前进入的参会者需要建立 WebRTC 连接
      await refreshPeerConnections()
    } catch (e) {
      message.warning(e.message || '无法打开摄像头和麦克风，但会议已开始')
    }
  } catch (e) {
    message.error('开始会议失败')
  }
}

function handleEndMeeting() {
  dialog.warning({
    title: '结束会议',
    content: '确定要结束会议吗？所有参会者将被移出。',
    positiveText: '结束',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        await meetingStore.endCurrentMeeting(meetingId.value)
        message.success('会议已结束')
        router.push('/home')
      } catch (e) {
        message.error('结束会议失败')
      }
    }
  })
}

function handleLeave() {
  dialog.warning({
    title: '离开会议',
    content: '确定要离开会议吗？',
    positiveText: '离开',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        await meetingStore.leaveCurrentMeeting(meetingId.value)
        webrtc.cleanup()
        router.push('/home')
      } catch (e) {
        message.error('离开会议失败')
      }
    }
  })
}

// 系统提示统一写入聊天面板，避免顶部弹窗刷屏
const recentSystemMessages = new Map()

function addSystemChatMessage(content) {
  const now = Date.now()
  const last = recentSystemMessages.get(content)
  // 同一内容 15 秒内只记录一次，防止事件重复触发造成刷屏
  if (last && now - last < 15000) return

  recentSystemMessages.set(content, now)
  if (recentSystemMessages.size > 100) {
    recentSystemMessages.clear()
  }

  meetingStore.addChatMessage({
    id: `sys-${now}-${Math.random().toString(36).slice(2, 8)}`,
    type: 'SYSTEM',
    content,
    senderName: '系统',
    timestamp: now
  })
}

function handleControlEvent(e) {
  const msg = e.detail
  if (msg.action === 'MUTE' || msg.action === 'UNMUTE') {
    // 其他用户的静音/取消静音状态变化
    loadParticipants()
  } else if (msg.action === 'MUTE_PARTICIPANT') {
    // 被主持人静音
    if (String(msg.targetUserId) === String(userStore?.userInfo?.id)) {
      if (webrtc.isMicOn.value) {
        webrtc.toggleMic(false) // 强制关闭麦克风
        syncAsrCaptureWithMic()
        message.warning('主持人已将您静音')
      }
    }
    loadParticipants()
  } else if (msg.action === 'UNMUTE_PARTICIPANT') {
    // 被主持人取消静音
    if (String(msg.targetUserId) === String(userStore?.userInfo?.id)) {
      message.info('主持人允许您发言，您可以开启麦克风')
    }
    loadParticipants()
  } else if (msg.action === 'REMOVE_PARTICIPANT') {
    // 被主持人移除
    if (String(msg.targetUserId) === String(userStore?.userInfo?.id)) {
      message.error('您已被主持人移除')
      setTimeout(() => {
        webrtc.cleanup()
        router.push('/home')
      }, 2000)
    } else {
      message.info(`${msg.senderName || '主持人'} 移除了一位参与者`)
      loadParticipants()
    }
  } else if (msg.action === 'MUTE_ALL') {
    // 全体静音
    if (String(msg.senderId) !== String(userStore?.userInfo?.id)) {
      if (webrtc.isMicOn.value) {
        webrtc.toggleMic(false)
        syncAsrCaptureWithMic()
        message.warning('主持人已开启全体静音')
      }
    }
    loadParticipants()
  } else if (msg.action === 'CAMERA_ON' || msg.action === 'CAMERA_OFF') {
    // 摄像头状态变化
    loadParticipants()
  } else if (msg.action === 'END_MEETING') {
    // 会议已结束
    message.warning('会议已结束')
    webrtc.cleanup()
    router.push('/home')
  } else if (msg.action === 'SCREEN_SHARE_START' || msg.action === 'SCREEN_SHARE_STOP') {
    // 屏幕共享状态变化
    loadParticipants()
  } else if (msg.action === 'USER_JOINED') {
    // 有新用户加入，重新加载参与者列表并建立连接
    loadParticipants()
    if (msg.senderId && String(msg.senderId) !== String(userStore?.userInfo?.id)) {
      addSystemChatMessage(`${msg.senderName || '用户'} 加入了会议`)
    }

    // 主动向新加入的用户发起WebRTC连接
    if (webrtc.localStream.value && msg.senderId) {
      const newUserId = msg.senderId.toString()
      if (newUserId !== userStore?.userInfo?.id?.toString()) {
        // 创建对等连接并发送offer
        webrtc.createPeerConnection(newUserId, (content, targetUserId) => {
          ws.sendSignaling(content, meetingId.value, targetUserId)
        })
        webrtc.createOffer(newUserId, (content, targetUserId) => {
          ws.sendSignaling(content, meetingId.value, targetUserId)
        })
      }
    }
  } else if (msg.action === 'USER_LEFT') {
    // 用户离开，清理对应的WebRTC连接
    if (msg.senderId) {
      webrtc.removePeerConnection(msg.senderId.toString())
    }
    loadParticipants()
    if (msg.senderId && String(msg.senderId) !== String(userStore?.userInfo?.id)) {
      addSystemChatMessage(`${msg.senderName || '用户'} 离开了会议`)
    }
  }
}

async function handleSignalingEvent(e) {
  const msg = e.detail
  try {
    // 后端下发经过旧版 MessageSendDto 包装：
    // { messageType: 'SIGNALING', content: '<整个 WsMessage 的 JSON>' }
    // 里面才是 { type:'SIGNALING', data:{ signalingType, sdp, ... } }
    // 兼容直接下发 WsMessage（msg.data）和旧版纯 JSON 字符串两种形态。
    let data = msg.data
    if (!data || !data.signalingType) {
      let parsed = msg.content
      if (typeof parsed === 'string') {
        try {
          parsed = JSON.parse(parsed)
        } catch (err) {
          return
        }
      }
      if (parsed && parsed.signalingType) {
        data = parsed
      } else if (parsed && parsed.data) {
        data = parsed.data
      }
    }
    if (!data || !data.signalingType) return

    const senderId = (data.fromUserId || msg.senderId)?.toString()
    console.log('[signaling] 收到', data.signalingType, '来自', senderId, 'sdp=', !!data.sdp, 'ice=', !!data.iceCandidate)

    // 后端转发的 data 是 SignalingMessage 对象，字段为 signalingType（大写枚举）
    if (data.signalingType === 'JOIN_ROOM') {
      // 有人加入会议：只刷新参会人列表。
      // 不在这里主动发 offer，否则双方同时发起 offer（glare）会导致协商失败；
      // 新加入者会自己向所有已参会者发 offer。
      await loadParticipants()
    } else if (data.signalingType === 'LEAVE_ROOM') {
      // 有人离开会议
      if (senderId) {
        webrtc.removePeerConnection(senderId)
      }
      await loadParticipants()
    } else if (data.signalingType === 'OFFER') {
      webrtc.handleOffer(senderId, data.sdp, (content, userId) => {
        ws.sendSignaling(content, meetingId.value, userId)
      })
    } else if (data.signalingType === 'ANSWER') {
      webrtc.handleAnswer(senderId, data.sdp)
    } else if (data.signalingType === 'ICE_CANDIDATE') {
      webrtc.handleIceCandidate(senderId, data.iceCandidate)
    }
  } catch (e) {
    console.error('处理信令消息失败', e)
  }
}

// 远端 <video> 元素登记表：重协商会更换 MediaStream 对象，
// 只靠模板 ref 不会重新赋值，这里用 watch 统一重新绑定 srcObject
const remoteVideoEls = new Map()

function bindRemoteVideo(userId, el) {
  if (el) {
    remoteVideoEls.set(userId, el)
    applyRemoteStream(userId)
  } else {
    remoteVideoEls.delete(userId)
  }
}

function applyRemoteStream(userId) {
  const el = remoteVideoEls.get(userId)
  const stream = webrtc.remoteStreams.value.get(userId)
  if (!el || !stream) return
  if (el.srcObject !== stream) {
    el.srcObject = stream
  }
  el.play?.().catch(() => {
    // 浏览器自动播放策略可能拦截带声音的视频，先静音保证画面可见，点击画面可恢复声音
    el.muted = true
    el.play?.().catch(() => {})
  })
}

function unmuteRemote(event) {
  const el = event.target
  el.muted = false
  el.play?.().catch(() => {})
}

watch(
  () => webrtc.remoteStreams.value,
  () => {
    remoteVideoEls.forEach((_, userId) => applyRemoteStream(userId))
  },
  { deep: true }
)

function getParticipantName(userId) {
  const p = meetingStore.participants.find(p => p.userId == userId)
  return p?.username || '用户'
}

function getRoleText(role) {
  const map = { host: '主持人', co_host: '联席主持人', participant: '参会者' }
  return map[role] || role
}

// 主持人控制功能
function muteParticipant(userId, shouldMute) {
  if (!isHost.value) {
    message.warning('只有主持人可以控制参与者')
    return
  }

  dialog.warning({
    title: shouldMute ? '静音参与者' : '取消静音',
    content: `确定要${shouldMute ? '静音' : '取消静音'}该参与者吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 先持久化静音状态，再通过 WebSocket 通知对方
        await muteParticipantApi(meetingId.value, userId, shouldMute)
        const action = shouldMute ? 'MUTE_PARTICIPANT' : 'UNMUTE_PARTICIPANT'
        ws.sendControl(action, meetingId.value, 'PRIVATE', userId.toString())
        message.success(shouldMute ? '已静音该参与者' : '已取消静音')
        await loadParticipants()
      } catch (e) {
        message.error(e.message || '操作失败')
      }
    }
  })
}

function removeParticipant(userId, displayName) {
  if (!isHost.value) {
    message.warning('只有主持人可以移除参与者')
    return
  }

  dialog.warning({
    title: '移除参与者',
    content: `确定要移除 ${displayName} 吗？该操作将强制其退出会议。`,
    positiveText: '移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 先更新数据库状态，再通知客户端
        await removeParticipantApi(meetingId.value, userId)
        ws.sendControl('REMOVE_PARTICIPANT', meetingId.value, 'PRIVATE', userId.toString())
        message.success(`已移除 ${displayName}`)
        // 刷新参与者列表
        setTimeout(() => loadParticipants(), 500)
      } catch (e) {
        message.error('移除失败')
      }
    }
  })
}

function muteAllParticipants() {
  if (!isHost.value) {
    message.warning('只有主持人可以执行此操作')
    return
  }

  dialog.warning({
    title: '全体静音',
    content: '确定要静音所有参与者吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      ws.sendControl('MUTE_ALL', meetingId.value, 'GROUP')
      message.success('已静音所有参与者')
      setTimeout(() => loadParticipants(), 500)
    }
  })
}

function startTimer() {
  // 先清除可能存在的旧定时器
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }

  // 使用会议的实际开始时间，如果没有则使用当前时间
  const startTime = meetingStore.currentMeeting?.actualStartTime
    ? new Date(meetingStore.currentMeeting.actualStartTime).getTime()
    : Date.now()

  // 立即更新一次
  meetingDuration.value = Math.floor((Date.now() - startTime) / 1000)

  // 然后每秒更新
  timer.value = setInterval(() => {
    meetingDuration.value = Math.floor((Date.now() - startTime) / 1000)
  }, 1000)
}

function stopTimer() {
  if (timer.value) {
    clearInterval(timer.value)
  }
}

// 切换字幕显示
async function toggleSubtitles() {
  showSubtitles.value = !showSubtitles.value

  if (showSubtitles.value) {
    // 启动ASR
    try {
      await startAsr(
        meetingId.value,
        userStore?.userInfo?.id,
        userStore?.userInfo?.username
      )

      // 启动音频采集
      audioCapture.value = new AudioCapture()
      audioCapture.value.setEnabled(webrtc.isMicOn.value)
      await audioCapture.value.start((pcmData) => {
        // 将 PCM 数据转换为 Base64 并发送
        const base64Audio = AudioCapture.pcmToBase64(pcmData)
        ws.sendAudio(base64Audio, meetingId.value)
      })

      message.success('字幕已开启')
    } catch (e) {
      console.error('启动字幕失败:', e)
      message.error(e.message || '启动字幕失败')
      showSubtitles.value = false

      // 清理音频采集
      if (audioCapture.value) {
        audioCapture.value.stop()
        audioCapture.value = null
      }
    }
  } else {
    // 停止ASR和音频采集
    try {
      // 停止音频采集
      if (audioCapture.value) {
        audioCapture.value.stop()
        audioCapture.value = null
      }

      await stopAsr(meetingId.value, userStore?.userInfo?.id)
      currentSubtitle.value = null
      currentTranslation.value = null
      message.info('字幕已关闭')
    } catch (e) {
      console.error('关闭字幕失败:', e)
      message.error('关闭字幕失败')
    }
  }
}

// 显示字幕（从WebSocket接收）
function showSubtitle(userName, text) {
  if (!showSubtitles.value) return

  currentSubtitle.value = { userName, text }
  // 新字幕到达时先清掉上一条译文，等本次翻译结果返回后再展示
  currentTranslation.value = null
  if (translationTimer.value) {
    clearTimeout(translationTimer.value)
    translationTimer.value = null
  }

  // 清除之前的定时器
  if (subtitleTimer.value) {
    clearTimeout(subtitleTimer.value)
  }

  // 5秒后自动隐藏字幕
  subtitleTimer.value = setTimeout(() => {
    currentSubtitle.value = null
  }, 5000)
}

// 处理字幕消息事件
function handleSubtitleEvent(e) {
  const { userName, text } = e.detail
  showSubtitle(userName, text)
}

// AI 实时翻译

function languageLabel(code) {
  const option = languageOptions.find(item => item.value === code)
  return option ? option.label : (code || '')
}

function toTranslationItem(detail) {
  return {
    key: `${detail.id ?? detail.transcriptId ?? Date.now()}-${detail.targetLanguage || ''}`,
    userName: detail.userName,
    targetLanguage: detail.targetLanguage,
    sourceText: detail.sourceText,
    translatedText: detail.translatedText
  }
}

async function loadAiSettings() {
  try {
    const res = await getAiSettings(meetingId.value)
    const data = res.data || {}
    aiSettings.value = {
      translationEnabled: !!data.translationEnabled,
      sourceLanguage: data.sourceLanguage || 'auto',
      targetLanguages: data.targetLanguages?.length ? data.targetLanguages : ['en'],
      summaryEnabled: data.summaryEnabled !== false,
      summaryLanguage: data.summaryLanguage || 'zh',
      autoSummary: data.autoSummary !== false
    }
    if (aiSettings.value.translationEnabled) {
      await loadTranslations()
    }
  } catch (e) {
    console.warn('加载 AI 配置失败:', e)
  }
}

async function saveAiSettings() {
  if (!isHost.value) return
  // 目标语种不能为空，恢复默认英语，保持前端展示与后端一致
  if (aiSettings.value.translationEnabled && !aiSettings.value.targetLanguages?.length) {
    aiSettings.value.targetLanguages = ['en']
    message.info('至少保留一个目标语言，已恢复为英语')
  }
  aiSettingsSaving.value = true
  try {
    await updateAiSettings(meetingId.value, aiSettings.value)
    message.success('AI 配置已更新')
    if (aiSettings.value.translationEnabled) {
      await loadTranslations()
    }
  } catch (e) {
    message.error(e.response?.data?.message || 'AI 配置更新失败')
  } finally {
    aiSettingsSaving.value = false
  }
}

async function loadTranslations() {
  try {
    const res = await getTranslations(meetingId.value)
    const list = Array.isArray(res.data) ? res.data : []
    translationRecords.value = list.slice(-50).reverse().map(toTranslationItem)
  } catch (e) {
    console.warn('加载翻译记录失败:', e)
  }
}

function handleTranslationEvent(e) {
  const detail = e.detail || {}
  if (!detail.translatedText) return

  const item = toTranslationItem(detail)
  if (!translationRecords.value.some(record => record.key === item.key)) {
    translationRecords.value = [item, ...translationRecords.value].slice(0, 50)
  }

  // 字幕开启时，在字幕下方展示本句的首个译文，忽略后续其他语种避免来回切换
  if (showSubtitles.value && currentSubtitle.value && !currentTranslation.value) {
    currentTranslation.value = item
    if (translationTimer.value) {
      clearTimeout(translationTimer.value)
    }
    translationTimer.value = setTimeout(() => {
      currentTranslation.value = null
    }, 6000)
  }
}

// AI 会议纪要

async function loadSummary() {
  try {
    const res = await getMeetingSummary(meetingId.value)
    if (res.data) {
      aiSummary.value = res.data
    }
    return res.data
  } catch (e) {
    console.warn('加载会议纪要失败:', e)
    return null
  }
}

function handleSummaryEvent(e) {
  const detail = e.detail
  if (!detail || !detail.status) return

  aiSummary.value = detail
  if (detail.status === 'success') {
    clearSummaryPoll()
    message.success('AI 会议纪要已生成')
  } else if (detail.status === 'failed') {
    clearSummaryPoll()
    message.error(detail.errorMessage || 'AI 会议纪要生成失败')
  }
}

async function triggerAiSummary() {
  try {
    const res = await triggerMeetingSummary(meetingId.value)
    if (res.data) {
      aiSummary.value = res.data
    }
    message.info('已提交纪要生成任务')
    startSummaryPoll()
  } catch (e) {
    message.error(e.response?.data?.message || '生成会议纪要失败')
  }
}

function startSummaryPoll() {
  clearSummaryPoll()
  summaryPollAttempts = 0
  summaryPollTimer.value = setInterval(async () => {
    summaryPollAttempts += 1
    const summary = await loadSummary()
    if (summary && ['success', 'failed'].includes(summary.status)) {
      clearSummaryPoll()
      return
    }
    if (summaryPollAttempts >= 15) {
      clearSummaryPoll()
      // 超时仍未完成时给出失败提示，避免按钮一直处于加载状态
      if (aiSummary.value && ['pending', 'generating'].includes(aiSummary.value.status)) {
        aiSummary.value = { ...aiSummary.value, status: 'failed', errorMessage: '生成超时，请稍后刷新重试' }
      }
    }
  }, 3000)
}

function clearSummaryPoll() {
  if (summaryPollTimer.value) {
    clearInterval(summaryPollTimer.value)
    summaryPollTimer.value = null
  }
}

function formatDuration(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function formatTime(ts) {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped lang="scss">
.meeting-room {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
  color: #1f2937;
}

.meeting-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid #e8ebf0;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meeting-topic {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.meeting-number {
  font-size: 12px;
  color: #6b7280;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 999px;
}

.topbar-center {
  .meeting-timer {
    font-size: 16px;
    font-weight: 600;
    color: #006eff;
    font-variant-numeric: tabular-nums;
  }
}

.video-area {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.subtitle-overlay {
  position: absolute;
  /* 底部悬浮工具条约占 20~95px，字幕整体上移避免被遮挡 */
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  max-width: 80%;
}

.subtitle-box {
  background: rgba(255, 255, 255, 0.92);
  color: #1f2937;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.5;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.18);
  animation: fadeIn 0.3s ease-in-out;
}

.subtitle-speaker {
  font-weight: 600;
  color: #4098FC;
  margin-right: 4px;
}

.subtitle-text {
  color: #1f2937;
}

.subtitle-line {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.subtitle-translation {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #e5e7eb;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.5;
}

.subtitle-translation-lang {
  display: inline-block;
  margin-right: 8px;
  padding: 0 6px;
  border-radius: 4px;
  background: rgba(0, 110, 255, 0.12);
  color: #006eff;
  font-size: 11px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  align-content: center;
  justify-content: center;
  gap: 14px;
  padding: 18px;
  overflow: hidden;
}

.video-cell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  max-height: 100%;
  background: linear-gradient(160deg, #1e293b 0%, #0f172a 100%);
  border: 1px solid rgba(255, 255, 255, 0.10);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.16);

  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .video-label {
    position: absolute;
    bottom: 12px;
    left: 12px;
    max-width: calc(100% - 24px);
    padding: 5px 12px;
    border-radius: 999px;
    background: rgba(12, 12, 24, 0.62);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(255, 255, 255, 0.08);
    font-size: 12.5px;
    color: #f8fafc;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.video-cell.participant-tile {
  flex-direction: column;
  gap: 12px;
}

.participant-avatar-lg {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3f8cff 0%, #006eff 100%);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 8px 24px rgba(0, 110, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 500;
  color: #fff;
}

.no-video-placeholder {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 40px;
  border: 1px dashed #e5e7eb;
  border-radius: 16px;
  background: #f1f5f9;

  .placeholder-avatar {
    width: 76px;
    height: 76px;
    border-radius: 50%;
    background: linear-gradient(135deg, #3f8cff 0%, #006eff 100%);
    border: 1px solid rgba(255, 255, 255, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30px;
    font-weight: 500;
    color: #fff;
  }
}

.chat-panel, .participants-panel, .ai-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #1f2937;
  border-left: 1px solid #e5e7eb;
  box-shadow: -6px 0 24px rgba(15, 23, 42, 0.06);
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 24px;
}

.ai-section {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.ai-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.ai-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.ai-count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #6b7280;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.ai-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ai-row-label {
  font-size: 13px;
  color: #6b7280;
}

.ai-tip {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.ai-empty {
  padding: 12px 0;
  font-size: 12px;
  line-height: 1.6;
  color: #9ca3af;
  text-align: center;
}

.translation-item {
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.translation-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 11px;
}

.translation-speaker {
  color: #006eff;
}

.translation-lang {
  padding: 0 6px;
  border-radius: 4px;
  background: rgba(0, 110, 255, 0.10);
  color: #006eff;
}

.translation-source {
  font-size: 12px;
  line-height: 1.5;
  color: #9ca3af;
  word-break: break-word;
}

.translation-text {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: #1f2937;
  word-break: break-word;
}

.summary-title {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.summary-overview {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.7;
  color: #6b7280;
}

.summary-block {
  margin-bottom: 12px;
}

.summary-block-title {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #006eff;
}

.summary-block ul {
  margin: 0;
  padding-left: 18px;
}

.summary-block li {
  margin-bottom: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #4b5563;
  word-break: break-word;
}

.action-owner {
  margin-left: 6px;
  color: #006eff;
  font-size: 11px;
}

.action-due {
  margin-left: 6px;
  color: #9ca3af;
  font-size: 11px;
}

.summary-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.chat-header, .panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.chat-message {
  margin-bottom: 12px;

  &.is-system {
    text-align: center;
    margin: 16px 0;

    .system-text {
      font-size: 12px;
      color: #9ca3af;
      background: #f3f4f6;
      padding: 4px 12px;
      border-radius: 10px;
    }
  }
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.msg-sender {
  font-size: 13px;
  font-weight: 500;
  color: #006eff;
}

.msg-time {
  font-size: 11px;
  color: #9ca3af;
}

.msg-content {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 12px;
  background: #f3f4f6;
  font-size: 14px;
  color: #1f2937;
  line-height: 1.5;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
}

.participant-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 12px;
  margin: 2px 8px;
  transition: background 0.2s;

  &:hover {
    background: #f3f4f6;
  }
}

.participant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3f8cff 0%, #006eff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.participant-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.participant-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 6px;
}

.host-badge {
  font-size: 11px;
  background: linear-gradient(135deg, #4f8bff 0%, #006eff 100%);
  color: white;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: normal;
}

.participant-role {
  font-size: 12px;
  color: #9ca3af;
}

.participant-status {
  display: flex;
  align-items: center;
  gap: 8px;

  .status-icon {
    font-size: 16px;
  }
}

.host-controls {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.participant-item:hover .host-controls {
  opacity: 1;
}

.meeting-toolbar {
  position: fixed;
  left: 50%;
  bottom: 20px;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: max-content;
  max-width: calc(100% - 40px);
  padding: 10px 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e5e7eb;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}

.meeting-room.has-sidebar .meeting-toolbar {
  left: calc(50% - 160px);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-center:not(:empty) {
  padding: 0 12px;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
}

.toolbar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 60px;
}

.toolbar-label {
  font-size: 11.5px;
  color: #4b5563;
  white-space: nowrap;
  user-select: none;
  letter-spacing: 0.2px;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ---------- 视觉细节打磨 ---------- */
.meeting-topbar {
  height: 60px;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.meeting-topic {
  max-width: 320px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meeting-number {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #38d39f;
  animation: live-pulse 2.4s ease-in-out infinite;

  &.is-idle {
    background: #cbd5e1;
    animation: none;
  }
}

@keyframes live-pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(56, 211, 159, 0.18); }
  50% { box-shadow: 0 0 0 7px rgba(56, 211, 159, 0.04); }
}

.topbar-center .meeting-timer {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(0, 110, 255, 0.12);
  border: 1px solid rgba(0, 110, 255, 0.22);
}

.topbar-right :deep(.n-button) {
  color: #4b5563;
}

.video-area {
  background:
    radial-gradient(700px 380px at 50% -15%, rgba(0, 110, 255, 0.06), transparent 70%);
}

.video-grid {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
  /* 底部留出悬浮工具条的空间，避免遮挡视频 */
  padding: 22px 24px 100px;
}

.video-cell {
  border-radius: 18px;
  box-shadow:
    0 18px 40px rgba(15, 23, 42, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);

  video {
    background: #0f172a;
  }
}

.local-video {
  border-color: rgba(0, 110, 255, 0.35);

  .video-label {
    background: rgba(0, 110, 255, 0.55);
    border-color: rgba(255, 255, 255, 0.24);
  }
}

.participant-avatar-lg {
  width: 84px;
  height: 84px;
  font-size: 30px;
  box-shadow: 0 10px 30px rgba(0, 110, 255, 0.40);
}

.toolbar-item {
  :deep(.n-button) {
    transition: transform 0.15s ease, background-color 0.15s ease;
  }

  :deep(.n-button--default-type) {
    color: #4b5563;
  }

  :deep(.n-button--default-type:hover) {
    transform: translateY(-1px);
    background: #f3f4f6;
  }
}

.toolbar-center :deep(.n-button) {
  height: 42px;
  padding: 0 26px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.3px;
  box-shadow: 0 8px 22px rgba(0, 110, 255, 0.35);
}

.toolbar-center :deep(.n-button--error-type) {
  box-shadow: 0 8px 22px rgba(255, 77, 79, 0.30);
}

.chat-header,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;

  :deep(.n-button) {
    color: #6b7280;
  }
}

.chat-messages,
.participants-list {
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 3px;
  }
}

.chat-input {
  padding: 12px 14px;
  gap: 10px;

  :deep(.n-input) {
    --n-border-radius: 999px;
  }
}

.msg-content {
  margin-top: 4px;
}
</style>
