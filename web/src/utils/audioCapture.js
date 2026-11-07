/**
 * 音频采集工具
 * 用于捕获麦克风音频并转换为 PCM 格式供 ASR 使用
 */

export class AudioCapture {
  constructor() {
    this.audioContext = null
    this.mediaStream = null
    this.sourceNode = null
    this.processorNode = null
    this.isCapturing = false
    this.enabled = true
    this.onDataCallback = null

    // ASR 要求的音频参数
    this.sampleRate = 16000
    this.channels = 1
  }

  /**
   * 开始音频采集
   * @param {Function} onData - 音频数据回调函数，参数为 PCM 数据 (Int16Array)
   */
  async start(onData) {
    if (this.isCapturing) {
      console.warn('音频采集已在运行中')
      return
    }

    this.onDataCallback = onData

    try {
      // 获取麦克风权限
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: this.sampleRate,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // 创建音频上下文
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: this.sampleRate
      })

      // 创建音频源节点
      this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream)

      // 使用 ScriptProcessorNode 处理音频数据
      // 缓冲区大小 1024（16kHz 下约 64ms），保证 Base64 后的消息体不超过 WebSocket 帧长限制
      const bufferSize = 1024
      this.processorNode = this.audioContext.createScriptProcessor(bufferSize, 1, 1)

      this.processorNode.onaudioprocess = (e) => {
        if (!this.isCapturing || !this.enabled) return

        const inputData = e.inputBuffer.getChannelData(0)
        const pcmData = this.floatToPCM16(inputData)

        if (this.onDataCallback) {
          this.onDataCallback(pcmData)
        }
      }

      // 连接节点
      this.sourceNode.connect(this.processorNode)
      this.processorNode.connect(this.audioContext.destination)

      this.isCapturing = true
      console.log('音频采集已启动', {
        sampleRate: this.audioContext.sampleRate,
        channels: this.channels
      })
    } catch (error) {
      console.error('启动音频采集失败:', error)
      this.cleanup()
      throw new Error('无法访问麦克风，请检查权限设置')
    }
  }

  /**
   * 控制是否上报音频帧（麦克风静音时不再采集上报）
   * @param {boolean} enabled
   */
  setEnabled(enabled) {
    this.enabled = !!enabled
  }

  /**
   * 停止音频采集
   */
  stop() {
    this.isCapturing = false
    this.cleanup()
    console.log('音频采集已停止')
  }

  /**
   * 清理资源
   */
  cleanup() {
    if (this.processorNode) {
      this.processorNode.disconnect()
      this.processorNode = null
    }

    if (this.sourceNode) {
      this.sourceNode.disconnect()
      this.sourceNode = null
    }

    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop())
      this.mediaStream = null
    }

    if (this.audioContext && this.audioContext.state !== 'closed') {
      this.audioContext.close()
      this.audioContext = null
    }

    this.onDataCallback = null
  }

  /**
   * 将 Float32Array 音频数据转换为 PCM16 (Int16Array)
   * @param {Float32Array} float32Array - 浮点音频数据 (-1.0 到 1.0)
   * @returns {Int16Array} PCM16 格式数据
   */
  floatToPCM16(float32Array) {
    const pcm16 = new Int16Array(float32Array.length)

    for (let i = 0; i < float32Array.length; i++) {
      // 限制在 -1.0 到 1.0 范围内
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      // 转换为 16 位整数 (-32768 到 32767)
      pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }

    return pcm16
  }

  /**
   * 将 Int16Array 转换为 Base64 字符串
   * @param {Int16Array} pcm16Data - PCM16 数据
   * @returns {string} Base64 编码的字符串
   */
  static pcmToBase64(pcm16Data) {
    const buffer = new ArrayBuffer(pcm16Data.length * 2)
    const view = new DataView(buffer)

    for (let i = 0; i < pcm16Data.length; i++) {
      view.setInt16(i * 2, pcm16Data[i], true) // true = 小端序
    }

    const bytes = new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i])
    }

    return btoa(binary)
  }
}
