import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createInstantMeeting,
  createScheduledMeeting,
  getMeetingDetail,
  joinMeeting,
  leaveMeeting,
  startMeeting,
  endMeeting,
  getMyMeetings,
  cancelMeeting
} from '@/api/meeting'

export const useMeetingStore = defineStore('meeting', () => {
  const currentMeeting = ref(null)
  const meetingList = ref([])
  const total = ref(0)
  const participants = ref([])
  const chatMessages = ref([])

  async function createInstant(data) {
    const res = await createInstantMeeting(data)
    currentMeeting.value = res.data
    return res.data
  }

  async function createScheduled(data) {
    const res = await createScheduledMeeting(data)
    currentMeeting.value = res.data
    return res.data
  }

  async function fetchMeetingDetail(meetingId) {
    const res = await getMeetingDetail(meetingId)
    currentMeeting.value = res.data
    return res.data
  }

  async function joinMeetingByNumber(meetingNumber, password) {
    const res = await joinMeeting({ meetingNumber, password })
    currentMeeting.value = res.data.meeting
    return res.data
  }

  async function leaveCurrentMeeting(meetingId) {
    await leaveMeeting(meetingId)
    currentMeeting.value = null
  }

  async function startCurrentMeeting(meetingId) {
    const res = await startMeeting(meetingId)
    currentMeeting.value = res.data
    return res.data
  }

  async function endCurrentMeeting(meetingId) {
    await endMeeting(meetingId)
    currentMeeting.value = null
  }

  async function cancelCurrentMeeting(meetingId) {
    await cancelMeeting(meetingId)
  }

  async function fetchMyMeetings(page = 1, size = 10, status) {
    const res = await getMyMeetings(page, size, status)
    // 后端返回的数据结构：{ data: [...], pagination: { totalItems: ... } }
    meetingList.value = res.data
    total.value = res.pagination?.totalItems || 0
    return {
      records: res.data,
      total: res.pagination?.totalItems || 0
    }
  }

  function addChatMessage(msg) {
    chatMessages.value.push(msg)
  }

  function clearChat() {
    chatMessages.value = []
  }

  function updateParticipants(list) {
    participants.value = list
  }

  return {
    currentMeeting, meetingList, total, participants, chatMessages,
    createInstant, createScheduled, fetchMeetingDetail, joinMeetingByNumber,
    leaveCurrentMeeting, startCurrentMeeting, endCurrentMeeting, cancelCurrentMeeting,
    fetchMyMeetings, addChatMessage, clearChat, updateParticipants
  }
})
