import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useMeetingStore } from '@/store/meeting'

export function useWebSocket() {
  const ws = ref(null)
  const connected = ref(false)
  const reconnectTimer = ref(null)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5
  const userStore = useUserStore()
  const heartbeatTimer = ref(null)
  // 主动断开（离开会议/组件卸载）时置为 true，避免 onclose 再触发自动重连
  let manuallyClosed = false
  // 连接建立后的回调（重连时同样会调用）
  let onOpenCallback = null

  function connect(meetingId, onOpen) {
    manuallyClosed = false
    if (typeof onOpen === 'function') {
      onOpenCallback = onOpen
    }
    const meetingStore = useMeetingStore()

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 默认走同源 /ws，由开发代理或生产环境反向代理转发到 WebSocket 服务端口；
    // 只有在显式配置 VITE_WS_PORT 时才直连指定端口
    const wsPort = import.meta.env.VITE_WS_PORT
    const wsHost = wsPort ? `${location.hostname}:${wsPort}` : location.host
    // 没有会议ID时按群聊等纯消息通道连接，避免后端把连接归入会议房间
    const sceneParam = meetingId ? `&meetingId=${meetingId}` : '&scene=chat'
    const wsUrl = `${protocol}//${wsHost}/ws?token=${userStore.token}${sceneParam}`

    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {
      connected.value = true
      reconnectAttempts.value = 0
      console.log('WebSocket 已连接')

      // 启动心跳
      startHeartbeat()

      // 连接（含重连）建立后回调，用于发送加入会议信令
      if (typeof onOpenCallback === 'function') {
        onOpenCallback()
      }
    }

    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        handleMessage(message, meetingStore)
      } catch (e) {
        console.error('解析消息失败', e)
      }
    }

    ws.value.onclose = (event) => {
      connected.value = false
      console.log('WebSocket 已断开', event?.code)
      stopHeartbeat()
      // 4001：同一账号在别处建立了新连接，本连接被服务端替换，不要自动重连
      if (event?.code === 4001) {
        manuallyClosed = true
        console.warn('当前账号已在其他页面重新连接，本页面停止实时通信')
        return
      }
      tryReconnect(meetingId)
    }

    ws.value.onerror = (error) => {
      console.error('WebSocket 错误', error)
    }
  }

  function handleMessage(message, meetingStore) {
    // 后端 MessageSendDto.content 是 WsMessage 的 JSON 字符串，需要解析
    let wsMsg = {}
    try {
      if (typeof message.content === 'string') {
        wsMsg = JSON.parse(message.content)
      }
    } catch (e) {
      // content 不是 JSON，保持原样
    }

    const messageType = message.type || message.messageType

    switch (messageType) {
      case 'TEXT':
      case 'SYSTEM':
        meetingStore.addChatMessage({
          id: Date.now(),
          senderId: wsMsg.senderId || message.senderId,
          senderName: wsMsg.senderName || message.senderName || '系统',
          content: wsMsg.content || message.content,
          type: messageType,
          timestamp: wsMsg.timestamp || message.timestamp
        })
        break
      case 'CONTROL':
        handleControlMessage({
          ...message,
          action: wsMsg.action || message.action,
          senderId: wsMsg.senderId || message.senderId,
          senderName: wsMsg.senderName || message.senderName,
          targetUserId: wsMsg.targetUserId || message.targetUserId,
          meetingId: wsMsg.meetingId || message.meetingId
        })
        break
      case 'SIGNALING':
        handleSignalingMessage(message)
        break
      case 'SUBTITLE':
        handleSubtitleMessage({
          userName: wsMsg.senderName || message.senderName,
          text: wsMsg.content || message.content
        })
        break
      case 'TRANSLATION':
        handleTranslationMessage(wsMsg)
        break
      case 'SUMMARY':
        handleSummaryMessage(wsMsg)
        break
      case 'GROUP_MESSAGE':
        handleGroupMessage(wsMsg)
        break
      case 'PRIVATE_MESSAGE':
        handlePrivateMessage(wsMsg)
        break
      case 'HEARTBEAT':
        break
      default:
        console.log('未知消息类型', message)
    }
  }

  function handleControlMessage(message) {
    const event = new CustomEvent('meeting-control', { detail: message })
    window.dispatchEvent(event)
  }

  function handleSubtitleMessage(detail) {
    const event = new CustomEvent('meeting-subtitle', { detail })
    window.dispatchEvent(event)
  }

  function handleTranslationMessage(detail) {
    if (!detail || !detail.translatedText) return
    const event = new CustomEvent('meeting-translation', { detail })
    window.dispatchEvent(event)
  }

  function handleSummaryMessage(detail) {
    const event = new CustomEvent('meeting-summary', { detail })
    window.dispatchEvent(event)
  }

  function handleGroupMessage(detail) {
    if (!detail || detail.groupId == null) return
    const event = new CustomEvent('group-message', { detail })
    window.dispatchEvent(event)
  }

  function handlePrivateMessage(detail) {
    if (!detail || detail.fromUserId == null) return
    const event = new CustomEvent('private-message', { detail })
    window.dispatchEvent(event)
  }

  function handleSignalingMessage(message) {
    const event = new CustomEvent('meeting-signaling', { detail: message })
    window.dispatchEvent(event)
  }

  function sendMessage(type, content, options = {}) {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) return

    const message = {
      type,
      content,
      timestamp: Date.now(),
      senderId: userStore.userInfo?.id?.toString(),
      ...options
    }

    ws.value.send(JSON.stringify(message))
  }

  function sendText(content, meetingId, targetScope = 'GROUP') {
    sendMessage('TEXT', content, { meetingId, targetScope })
  }

  function sendControl(action, meetingId, targetScope = 'GROUP', targetUserId) {
    sendMessage('CONTROL', action, { meetingId, action, targetScope, targetUserId })
  }

  function sendSignaling(content, meetingId, receiverId) {
    const parsed = JSON.parse(content)
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      console.warn('[ws] 未连接，信令被丢弃:', parsed.type, '→', receiverId)
      return
    }

    // 构造 SignalingMessage 格式（匹配后端 SignalingMessage 类）
    const signalingData = {
      signalingType: parsed.type.toUpperCase().replace(/-/g, '_'), // 'ice-candidate' -> 'ICE_CANDIDATE'
      meetingId: meetingId?.toString(),
      fromUserId: userStore.userInfo?.id?.toString(),
      toUserId: receiverId?.toString(),
      sdp: parsed.sdp || null,
      // 兼容两种写法：useWebRTC 里发的是 iceCandidate，其他调用方用的是 candidate
      iceCandidate: parsed.iceCandidate || parsed.candidate || null
    }

    const message = {
      type: 'SIGNALING',
      content: JSON.stringify(signalingData),
      senderId: userStore.userInfo?.id?.toString(),
      meetingId: meetingId?.toString(),
      receiverId: receiverId?.toString(),
      timestamp: Date.now()
    }

    ws.value.send(JSON.stringify(message))
    console.log('[ws] 发送信令', signalingData.signalingType, '→', receiverId)
  }

  function sendHeartbeat() {
    sendMessage('HEARTBEAT', 'ping')
  }

  function sendAudio(audioData, meetingId) {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) return

    const message = {
      type: 'AUDIO',
      content: audioData,
      meetingId,
      senderId: userStore.userInfo?.id?.toString(),
      senderName: userStore.userInfo?.username,
      timestamp: Date.now()
    }

    ws.value.send(JSON.stringify(message))
  }

  function startHeartbeat() {
    // 每 30 秒发送一次心跳
    heartbeatTimer.value = setInterval(() => {
      if (connected.value) {
        sendHeartbeat()
      }
    }, 30000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer.value) {
      clearInterval(heartbeatTimer.value)
      heartbeatTimer.value = null
    }
  }

  function tryReconnect(meetingId) {
    if (manuallyClosed) {
      return
    }
    if (reconnectAttempts.value >= maxReconnectAttempts) {
      console.log('达到最大重连次数')
      return
    }

    // 清理旧连接，先摘掉 onclose 防止再次触发重连造成定时器叠加
    if (ws.value) {
      ws.value.onclose = null
      try {
        ws.value.close()
      } catch (e) {
        // ignore
      }
      ws.value = null
    }

    reconnectTimer.value = setTimeout(() => {
      reconnectAttempts.value++
      console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})`)
      connect(meetingId)
    }, 2000 * reconnectAttempts.value)
  }

  function disconnect() {
    manuallyClosed = true
    onOpenCallback = null
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
    stopHeartbeat()
    if (ws.value) {
      ws.value.onclose = null
      ws.value.close()
      ws.value = null
    }
    reconnectAttempts.value = 0
    connected.value = false
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    connect,
    disconnect,
    sendMessage,
    sendText,
    sendControl,
    sendSignaling,
    sendHeartbeat,
    sendAudio
  }
}
