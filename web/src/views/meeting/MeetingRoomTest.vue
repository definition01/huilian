<template>
  <div style="padding: 20px;">
    <h1>会议室测试页面</h1>
    <p>会议ID: {{ meetingId }}</p>
    <p>加载状态: {{ loading ? '加载中...' : '已加载' }}</p>
    <p v-if="error" style="color: red;">错误: {{ error }}</p>
    <p v-if="meetingInfo">会议信息: {{ meetingInfo?.topic }}</p>

    <div style="margin-top: 20px;">
      <button @click="testCamera">测试摄像头</button>
      <button @click="testMic" style="margin-left: 10px;">测试麦克风</button>
    </div>

    <div v-if="cameraStatus" style="margin-top: 10px; color: green;">
      {{ cameraStatus }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useMeetingStore } from '@/store/meeting'

const route = useRoute()
const meetingStore = useMeetingStore()

const meetingId = ref(route.params.meetingId)
const loading = ref(true)
const error = ref(null)
const meetingInfo = ref(null)
const cameraStatus = ref('')

onMounted(async () => {
  try {
    const data = await meetingStore.fetchMeetingDetail(meetingId.value)
    meetingInfo.value = data
    loading.value = false
  } catch (e) {
    error.value = e.message || '加载失败'
    loading.value = false
    console.error('错误:', e)
  }
})

async function testCamera() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    cameraStatus.value = '摄像头和麦克风访问成功！'
    setTimeout(() => {
      stream.getTracks().forEach(track => track.stop())
    }, 3000)
  } catch (e) {
    cameraStatus.value = '失败: ' + e.message
  }
}

async function testMic() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    cameraStatus.value = '麦克风访问成功！'
    setTimeout(() => {
      stream.getTracks().forEach(track => track.stop())
    }, 3000)
  } catch (e) {
    cameraStatus.value = '失败: ' + e.message
  }
}
</script>
