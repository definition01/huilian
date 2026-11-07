# 火山引擎ASR集成说明

## 功能概述

已集成火山引擎ASR（自动语音识别）功能，实现：
1. ✅ 实时语音转文字字幕显示
2. ✅ 会议记录保存到数据库
3. ✅ 会议记录查询和管理

## 配置步骤

### 1. 配置火山引擎ASR凭证

在 `application.yml` 或环境变量中配置：

```yaml
volcengine:
  asr:
    enabled: true  # 启用ASR功能
    app-id: your-app-id  # 火山引擎应用ID
    access-key: your-access-key  # 访问密钥
    secret-key: your-secret-key  # 秘密密钥
    endpoint: wss://openspeech.bytedance.com/api/v1/asr
    language: zh-CN  # 识别语言
    format: pcm  # 音频格式
    sample-rate: 16000  # 采样率
```

或使用环境变量：
```bash
export VOLCENGINE_ASR_ENABLED=true
export VOLCENGINE_ASR_APP_ID=your-app-id
export VOLCENGINE_ASR_ACCESS_KEY=your-access-key
export VOLCENGINE_ASR_SECRET_KEY=your-secret-key
```

### 2. 运行数据库迁移

数据库迁移脚本已创建：`V1_7__create_meeting_transcript.sql`

如果使用Flyway，会自动执行。否则手动执行：

```sql
CREATE TABLE IF NOT EXISTS meeting_transcript (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    text_content TEXT NOT NULL,
    start_time INT NOT NULL,
    end_time INT NOT NULL,
    confidence DOUBLE DEFAULT 1.0,
    is_final BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3. 更新Maven依赖

POM文件已更新，在IDEA中刷新Maven：
- 右键点击项目
- Maven -> Reload Project

### 4. 重启后端服务器

在IDEA中重新运行SpringBoot应用。

### 5. 重启前端服务器

前端已在3001端口运行，代码已自动更新。

## 使用方法

### 前端使用

1. **进入会议室**
2. **点击底部工具栏的"开启字幕"按钮**（字幕图标）
3. **开始说话**，实时字幕会显示在视频下方
4. **点击"关闭字幕"按钮**停止识别

### API接口

#### 获取会议转录记录
```http
GET /api/transcript/{meetingId}?finalOnly=false
```

#### 删除会议转录记录
```http
DELETE /api/transcript/{meetingId}
```

#### 启动ASR
```http
POST /api/transcript/start?meetingId=1&userId=1&userName=张三
```

#### 停止ASR
```http
POST /api/transcript/stop?meetingId=1&userId=1
```

## 技术架构

### 后端
- **VolcengineAsrService**: ASR服务类，管理WebSocket连接
- **MeetingTranscriptService**: 会议记录服务，数据库CRUD
- **MeetingTranscriptController**: REST API控制器
- **MeetingTranscript**: 会议记录实体类

### 前端
- **会议室组件**: 集成字幕显示UI
- **字幕开关**: 工具栏按钮控制
- **实时显示**: 接收ASR结果并显示字幕
- **自动隐藏**: 字幕5秒后自动消失

### 数据流
1. 用户点击"开启字幕" → 前端调用 `/api/transcript/start`
2. 后端创建WebSocket连接到火山引擎ASR
3. 前端采集音频 → 发送到后端 → 转发到ASR
4. ASR识别结果 → 后端保存数据库 → WebSocket推送到前端
5. 前端显示实时字幕

## 注意事项

1. **音频格式**: 前端需要将麦克风音频转换为PCM 16kHz格式
2. **网络连接**: ASR需要稳定的网络连接
3. **成本控制**: 火山引擎ASR按使用时长计费，注意成本
4. **隐私保护**: 会议记录包含敏感信息，注意权限控制
5. **性能优化**: 大量并发用户时注意服务器性能

## 下一步优化

- [ ] 实现音频流采集和编码（前端WebRTC音频）
- [ ] 添加会议记录导出功能（Word/PDF）
- [ ] 实现会议纪要自动生成（AI摘要）
- [ ] 支持多语言识别切换
- [ ] 添加识别准确率统计
- [ ] 实现说话人分离（区分不同参会人）

## 故障排查

### ASR连接失败
- 检查凭证配置是否正确
- 检查网络是否能访问火山引擎服务
- 查看后端日志错误信息

### 字幕不显示
- 确认ASR功能已启用（enabled=true）
- 检查浏览器控制台是否有错误
- 确认麦克风权限已授予

### 识别不准确
- 检查音频采样率是否为16kHz
- 确保环境安静，减少背景噪音
- 说话清晰，语速适中

## 联系支持

如有问题，请查看：
- 火山引擎ASR文档: https://www.volcengine.com/docs/6561/79817
- 项目issue: https://github.com/your-repo/issues
