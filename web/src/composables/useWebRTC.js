import { ref, onUnmounted } from 'vue'

export function useWebRTC() {
  const localStream = ref(null)
  const remoteStreams = ref(new Map())
  const peerConnections = ref(new Map())
  const isCameraOn = ref(false)
  const isMicOn = ref(false)
  const isScreenSharing = ref(false)
  const screenStream = ref(null)
  // 远程描述设置完成前到达的 ICE 候选需要先缓存，否则 addIceCandidate 会抛错并丢失候选
  const pendingCandidates = new Map()
  // 防止同一用户并发发起 offer
  const offerInFlight = new Set()
  // 记录连接创建时间，用于清理长时间没协商成功的僵尸连接
  const pcCreatedAt = new Map()
  // 当前登录用户ID，用于在 glare 时决定谁是 polite 方
  const localUserId = ref(null)

  function setLocalUserId(id) {
    localUserId.value = id == null ? null : String(id)
  }

  const iceServers = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  }

  async function startCamera() {
    try {
      // 检查浏览器是否支持 mediaDevices API
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('您的浏览器不支持摄像头和麦克风功能，请使用最新版本的 Chrome、Firefox 或 Edge 浏览器')
      }

      // 优先请求 1080p / 30fps，摄像头不支持时会自动回退到最接近的分辨率；
      // 默认 getUserMedia 往往只给 640x480，再叠加 VP8 默认的低码率，画面会明显发糊
      localStream.value = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1920 },
          height: { ideal: 1080 },
          frameRate: { ideal: 30, max: 30 },
          facingMode: 'user'
        },
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // detail 提示编码器优先保留画面细节（人脸、文字更锐利），而不是一味保帧率
      const videoTrack = localStream.value.getVideoTracks()[0]
      if (videoTrack) {
        try {
          videoTrack.contentHint = 'detail'
        } catch (e) {
          console.warn('设置视频 contentHint 失败', e)
        }
        const settings = videoTrack.getSettings ? videoTrack.getSettings() : {}
        console.log('摄像头采集参数', {
          width: settings.width,
          height: settings.height,
          frameRate: settings.frameRate
        })
      }
      isCameraOn.value = true
      isMicOn.value = true
      return localStream.value
    } catch (e) {
      console.error('获取摄像头失败', e)

      // 根据错误类型提供友好提示
      let errorMessage = '无法打开摄像头和麦克风'

      if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
        errorMessage = '您拒绝了摄像头和麦克风权限。请在浏览器地址栏左侧点击锁图标，允许使用摄像头和麦克风权限。'
      } else if (e.name === 'NotFoundError' || e.name === 'DevicesNotFoundError') {
        errorMessage = '未检测到摄像头或麦克风设备。请检查设备是否已正确连接。'
      } else if (e.name === 'NotReadableError' || e.name === 'TrackStartError') {
        errorMessage = '摄像头或麦克风已被其他应用占用。请关闭其他正在使用这些设备的应用后重试。'
      } else if (e.name === 'OverconstrainedError' || e.name === 'ConstraintNotSatisfiedError') {
        errorMessage = '无法满足摄像头和麦克风的要求。请检查设备配置。'
      } else if (e.name === 'TypeError') {
        errorMessage = '浏览器配置错误。请确保使用 HTTPS 访问或在 localhost 环境下。'
      } else if (e.message) {
        errorMessage = e.message
      }

      const error = new Error(errorMessage)
      error.originalError = e
      throw error
    }
  }

  async function stopCamera() {
    if (localStream.value) {
      localStream.value.getTracks().forEach(track => track.stop())
      localStream.value = null
    }
    isCameraOn.value = false
    isMicOn.value = false
  }

  function toggleCamera() {
    if (localStream.value) {
      const videoTrack = localStream.value.getVideoTracks()[0]
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled
        isCameraOn.value = videoTrack.enabled
      }
    }
  }

  function toggleMic(forceState) {
    if (localStream.value) {
      const audioTrack = localStream.value.getAudioTracks()[0]
      if (audioTrack) {
        // 如果提供了 forceState 参数，强制设置为该状态
        // 否则切换当前状态
        if (forceState !== undefined) {
          audioTrack.enabled = forceState
          isMicOn.value = forceState
        } else {
          audioTrack.enabled = !audioTrack.enabled
          isMicOn.value = audioTrack.enabled
        }
      }
    }
  }

  async function startScreenShare() {
    try {
      screenStream.value = await navigator.mediaDevices.getDisplayMedia({
        video: true,
        audio: false
      })

      const videoTrack = screenStream.value.getVideoTracks()[0]

      peerConnections.value.forEach(pc => {
        const sender = pc.getSenders().find(s => s.track?.kind === 'video')
        if (sender) {
          sender.replaceTrack(videoTrack)
        }
      })

      videoTrack.onended = () => {
        stopScreenShare()
      }

      isScreenSharing.value = true
      return screenStream.value
    } catch (e) {
      console.error('屏幕共享失败', e)
      throw e
    }
  }

  function stopScreenShare() {
    if (screenStream.value) {
      screenStream.value.getTracks().forEach(track => track.stop())
      screenStream.value = null
    }

    if (localStream.value) {
      const videoTrack = localStream.value.getVideoTracks()[0]
      peerConnections.value.forEach(pc => {
        const sender = pc.getSenders().find(s => s.track?.kind === 'video')
        if (sender && videoTrack) {
          sender.replaceTrack(videoTrack)
        }
      })
    }

    isScreenSharing.value = false
  }

  // 提升视频发送质量：给 video sender 设定码率上限并优先保清晰度，
  // 默认编码参数在高分辨率下码率过低会导致画面模糊、拖影
  async function tuneVideoSender(pc) {
    const sender = pc.getSenders().find(s => s.track?.kind === 'video')
    if (!sender || typeof sender.getParameters !== 'function') return

    try {
      const params = sender.getParameters()
      if (!params.encodings || params.encodings.length === 0) {
        params.encodings = [{}]
      }
      params.encodings.forEach(encoding => {
        encoding.maxBitrate = 4000000
        encoding.maxFramerate = 30
        encoding.scaleResolutionDownBy = 1
        // 网络分配时优先保障视频，避免一拥塞就降分辨率
        encoding.priority = 'high'
      })
      params.degradationPreference = 'maintain-resolution'
      await sender.setParameters(params)
    } catch (e) {
      console.warn('设置视频发送参数失败', e)
    }
  }

  function createPeerConnection(userId, sendSignaling) {
    // 已存在连接则直接复用，避免重复创建导致资源泄漏
    const existing = peerConnections.value.get(userId)
    if (existing) {
      return existing
    }

    const pc = new RTCPeerConnection(iceServers)

    if (localStream.value) {
      localStream.value.getTracks().forEach(track => {
        pc.addTrack(track, localStream.value)
      })
      // 轨道挂上后再调整编码参数，否则 getSenders 里还拿不到 video sender
      tuneVideoSender(pc)
    }

    // 即使当前没有摄像头/麦克风也补齐收发通道，
    // 否则 SDP 里没有对应 m-line，对方有画面也无处可发
    ensureMediaTransceivers(pc)

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        // 后端 SignalingMessage 的字段名是 iceCandidate（不是 candidate），
        // 用错字段名会被 Jackson 丢弃，导致双方无法交换候选、只有 SDP 没有媒体
        sendSignaling(JSON.stringify({
          type: 'ice-candidate',
          iceCandidate: {
            candidate: event.candidate.candidate,
            sdpMid: event.candidate.sdpMid,
            sdpMLineIndex: event.candidate.sdpMLineIndex
          }
        }), userId)
      }
    }

    pc.ontrack = (event) => {
      console.log('[webrtc] ontrack 收到远端轨道', userId, event.track.kind, 'streams=', event.streams.length)
      let stream = event.streams[0]
      if (!stream) {
        // 部分浏览器（或某些重协商场景）不会带 streams，需要自己组装
        stream = remoteStreams.value.get(userId) || new MediaStream()
        stream.addTrack(event.track)
      } else {
        const existing = remoteStreams.value.get(userId)
        if (existing && existing.id !== stream.id) {
          // 重协商后可能换成新的 MediaStream，保留已有轨道避免画面丢失
          existing.getTracks().forEach(t => {
            if (!stream.getTracks().some(x => x.id === t.id)) stream.addTrack(t)
          })
        }
      }
      // 赋值新 Map 触发视图更新，让远程 video 重新绑定 srcObject
      remoteStreams.value = new Map(remoteStreams.value).set(userId, stream)
    }

    pc.onconnectionstatechange = () => {
      console.log('[webrtc] 连接状态', userId, pc.connectionState)
      if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
        removePeerConnection(userId)
      }
    }

    peerConnections.value.set(userId, pc)
    pcCreatedAt.set(userId, Date.now())
    return pc
  }

  /**
   * 确保连接里有音频/视频的收发通道。
   * 否则发起 offer 的一方如果还没开摄像头，SDP 里没有 video m-line，
   * 对方即使有摄像头也没有位置可以发送轨道（表现为“无视频轨”）
   */
  function ensureMediaTransceivers(pc) {
    const kinds = pc.getTransceivers()
      .map(t => (t.receiver?.track?.kind) || (t.sender?.track?.kind))
      .filter(Boolean)

    if (!kinds.includes('audio')) {
      pc.addTransceiver('audio', { direction: 'sendrecv' })
    }
    if (!kinds.includes('video')) {
      pc.addTransceiver('video', { direction: 'sendrecv' })
    }
  }

  /**
   * 把本地音视频轨道同步到已有连接。
   * 应答方（非 offer 发起方）用 replaceTrack 即可补上轨道，不需要重新协商
   */
  async function syncLocalTracksToPeers() {
    const stream = localStream.value
    if (!stream) return

    for (const pc of peerConnections.value.values()) {
      for (const track of stream.getTracks()) {
        const sender = pc.getSenders().find(s => {
          if (s.track?.kind === track.kind) return true
          const transceiver = pc.getTransceivers().find(t => t.sender === s)
          return (transceiver?.receiver?.track?.kind || transceiver?.sender?.track?.kind) === track.kind
        })

        if (sender) {
          if (sender.track !== track) {
            try {
              await sender.replaceTrack(track)
            } catch (e) {
              console.warn('替换本地轨道失败', e)
            }
          }
        } else {
          pc.addTrack(track, stream)
        }
      }

      // 新增或替换轨道后重新应用码率/清晰度参数
      await tuneVideoSender(pc)
    }
  }

  /**
   * 对已有连接重新发起协商（本地新增/移除轨道后调用）。
   * 只有连接处于 stable 时才发起，正在协商中则跳过，等对方处理
   */
  async function renegotiate(userId, sendSignaling) {
    const pc = peerConnections.value.get(userId)
    if (!pc || pc.signalingState !== 'stable' || offerInFlight.has(userId)) {
      return
    }

    offerInFlight.add(userId)
    try {
      ensureMediaTransceivers(pc)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      sendSignaling(JSON.stringify({
        type: 'offer',
        sdp: offer
      }), userId)
      console.log('[webrtc] 重新协商 offer', userId)
    } catch (e) {
      console.warn('[webrtc] 重新协商失败', userId, e)
    } finally {
      offerInFlight.delete(userId)
    }
  }

  async function createOffer(userId, sendSignaling) {
    const pc = peerConnections.value.get(userId)
    if (!pc) return

    // 已有本地描述说明 offer 已发送，避免重复
    if (pc.localDescription || offerInFlight.has(userId)) {
      console.log('[webrtc] 跳过发起 offer', userId, 'localDescription=', pc.localDescription?.type)
      return
    }

    offerInFlight.add(userId)
    try {
      ensureMediaTransceivers(pc)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)

      sendSignaling(JSON.stringify({
        type: 'offer',
        sdp: offer
      }), userId)
    } finally {
      offerInFlight.delete(userId)
    }
  }

  async function handleOffer(userId, sdp, sendSignaling) {
    console.log('[webrtc] 收到 OFFER 来自', userId, '当前状态', peerConnections.value.get(userId)?.signalingState)
    let pc = peerConnections.value.get(userId)
    const polite = localUserId.value == null || Number(localUserId.value) < Number(userId)

    if (pc) {
      if (pc.signalingState === 'have-local-offer') {
        // 双方同时重新协商（glare）：小的 userId（polite）回滚自己的 offer 接受对方，
        // 大的 userId 忽略对方的 offer，保证同一时刻只有一份 offer 在协商
        if (!polite) {
          console.warn('[webrtc] 忽略同时到达的 offer', userId)
          return
        }
        await pc.setLocalDescription({ type: 'rollback' })
      } else if (pc.signalingState !== 'stable') {
        console.warn('[webrtc] 当前状态不接受 offer', userId, pc.signalingState)
        return
      }
    }

    if (!pc) {
      pc = createPeerConnection(userId, sendSignaling)
    }

    await pc.setRemoteDescription(new RTCSessionDescription(sdp))
    await flushPendingCandidates(userId, pc)

    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)

    sendSignaling(JSON.stringify({
      type: 'answer',
      sdp: answer
    }), userId)
  }

  async function handleAnswer(userId, sdp) {
    const pc = peerConnections.value.get(userId)
    if (!pc) return

    // 已经重建连接时，旧 offer 对应的 answer 会迟到，直接忽略避免抛错
    if (pc.signalingState !== 'have-local-offer') {
      console.warn('[webrtc] 忽略过期的 answer', userId, pc.signalingState)
      return
    }

    await pc.setRemoteDescription(new RTCSessionDescription(sdp))
    await flushPendingCandidates(userId, pc)
  }

  async function handleIceCandidate(userId, candidate) {
    if (!candidate) return

    const pc = peerConnections.value.get(userId)
    // 远端描述还没设置时先缓存，等 handleOffer/handleAnswer 完成后再统一加入
    if (!pc || !pc.remoteDescription) {
      const list = pendingCandidates.get(userId) || []
      list.push(candidate)
      pendingCandidates.set(userId, list)
      return
    }

    try {
      await pc.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (e) {
      console.warn('添加 ICE 候选失败', e)
    }
  }

  async function flushPendingCandidates(userId, pc) {
    const list = pendingCandidates.get(userId)
    if (!list || list.length === 0) return

    pendingCandidates.delete(userId)
    for (const candidate of list) {
      try {
        await pc.addIceCandidate(new RTCIceCandidate(candidate))
      } catch (e) {
        console.warn('添加缓存的 ICE 候选失败', e)
      }
    }
  }

  function resetPeerConnections() {
    peerConnections.value.forEach(pc => pc.close())
    peerConnections.value.clear()
    remoteStreams.value.clear()
    pendingCandidates.clear()
    offerInFlight.clear()
    pcCreatedAt.clear()
  }

  /**
   * 清理长时间停留在“已有本地描述但没收到任何远端描述”的连接。
   * 这类连接通常是 offer/answer 丢失，重新建立才有机会恢复
   */
  function cleanupStaleConnections(timeout = 8000) {
    const now = Date.now()
    for (const [userId, pc] of Array.from(peerConnections.value.entries())) {
      const createdAt = pcCreatedAt.get(userId) || 0
      if (!pc.remoteDescription && now - createdAt > timeout) {
        console.warn('[webrtc] 清理长时间未协商的连接', userId)
        removePeerConnection(userId)
      }
    }
  }

  function removePeerConnection(userId) {
    const pc = peerConnections.value.get(userId)
    if (pc) {
      pc.close()
      peerConnections.value.delete(userId)
    }
    pendingCandidates.delete(userId)
    offerInFlight.delete(userId)
    pcCreatedAt.delete(userId)
    remoteStreams.value.delete(userId)
  }

  function cleanup() {
    peerConnections.value.forEach(pc => pc.close())
    peerConnections.value.clear()
    remoteStreams.value.clear()
    pendingCandidates.clear()
    offerInFlight.clear()
    if (localStream.value) {
      localStream.value.getTracks().forEach(track => track.stop())
      localStream.value = null
    }
    if (screenStream.value) {
      screenStream.value.getTracks().forEach(track => track.stop())
      screenStream.value = null
    }
    isCameraOn.value = false
    isMicOn.value = false
    isScreenSharing.value = false
  }

  onUnmounted(() => {
    cleanup()
  })

  return {
    localStream,
    remoteStreams,
    peerConnections,
    isCameraOn,
    isMicOn,
    isScreenSharing,
    startCamera,
    stopCamera,
    toggleCamera,
    toggleMic,
    startScreenShare,
    stopScreenShare,
    createPeerConnection,
    createOffer,
    renegotiate,
    syncLocalTracksToPeers,
    handleOffer,
    handleAnswer,
    handleIceCandidate,
    removePeerConnection,
    cleanupStaleConnections,
    resetPeerConnections,
    setLocalUserId,
    cleanup
  }
}
