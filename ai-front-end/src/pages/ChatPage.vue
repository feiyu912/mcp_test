<template>
  <div class="chat-layout">
    <div class="chat-sessions">
      <div class="session-header">
        <span style="font-weight:600; font-size:1.1rem;">历史会话</span>
        <el-button circle size="small" type="primary" @click="createSession" style="margin-left:auto; font-size:15px; font-weight:500; letter-spacing:2px; width:40px; height:40px; display:flex; align-items:center; justify-content:center;">新建</el-button>
      </div>
      <el-scrollbar class="session-list">
        <div v-for="session in sessions" :key="session.id" :class="['session-item', session.id === currentSessionId ? 'active' : '']">
          <div class="session-item-main" @click="switchSession(session.id)">
            <span v-if="editSessionId !== session.id" class="session-title">{{ session.title }}</span>
            <el-input v-else v-model="editSessionTitle" size="small" @blur="saveSessionTitle(session)" @keyup.enter="saveSessionTitle(session)" style="width:110px;" />
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <el-popconfirm title="确定删除该会话？" @confirm="deleteSession(session.id)">
            <template #reference>
              <span class="session-action session-x" @click.stop>x</span>
            </template>
          </el-popconfirm>
        </div>
      </el-scrollbar>
    </div>
    <div class="chat-page">
      <div class="chat-header">
        <el-icon style="vertical-align: middle;"><ChatLineRound /></el-icon>
        <span style="margin-left: 8px; font-size: 1.2rem; font-weight: 600; color: #1976d2;">AI 对话</span>
      </div>
      <div class="chat-messages">
        <div v-for="(msg, idx) in messages" :key="idx">
          <!-- 只在有参考内容的AI回复上方显示参考栏 -->
          <template v-if="msg.role === 'assistant' && msg.reference && msg.reference.length">
            <div class="reference-bar" style="margin-bottom: 8px;">
              <b>本轮AI参考（共{{ msg.reference.length }}条）</b>
              <el-button size="small" type="text" @click="msg.referenceCollapsed = !msg.referenceCollapsed" style="margin-left: 8px;">
                {{ msg.referenceCollapsed ? '展开' : '收起' }}
              </el-button>
              <div v-show="!msg.referenceCollapsed">
                <span v-for="(item, ridx) in msg.reference" :key="ridx" class="ref-item">
                  <span v-if="item.source === 'session'">
                    <el-tag type="success">对话文件</el-tag>
                    <b>{{ item.fileName }}</b>
                    <span v-if="item.segmentNo">(第{{ item.segmentNo }}段)</span>
                    <span class="ref-session">{{ item.text.slice(0, 30) }}...</span>
                  </span>
                  <span v-else>
                    <el-tag>全局知识</el-tag>
                    <span class="ref-global">{{ item.text.slice(0, 30) }}...</span>
                  </span>
                  <span v-if="ridx < msg.reference.length - 1">，</span>
                </span>
              </div>
            </div>
          </template>
          <div :class="['msg', msg.role]">
            <div class="msg-bubble">
              <b>{{ msg.role === 'user' ? '我' : 'AI' }}：</b>
              <span v-if="msg.role === 'assistant'" style="white-space: pre-line;">{{ removeMarkdownBold(msg.content) }}</span>
              <span v-else>{{ msg.content }}</span>
            </div>
            <!-- MCP工具调用信息显示 -->
            <div v-if="msg.role === 'assistant' && msg.mcpContent" class="mcp-content">
              <div class="mcp-header">
                <el-icon><Tools /></el-icon>
                <span>MCP工具调用</span>
              </div>
              <div class="mcp-body">
                <pre>{{ msg.mcpContent }}</pre>
              </div>
            </div>
            <!-- 知识库引用内容显示 -->
            <!-- <div v-if="msg.role === 'assistant' && msg.reference && msg.reference.length" class="reference-bar">
              <div v-for="ref in msg.reference" :key="ref.text" class="ref-item">
                <span :class="ref.source === 'global' ? 'ref-global' : 'ref-session'">
                  {{ ref.text }}
                </span>
              </div>
            </div> -->
          </div>
        </div>
        <div v-if="streaming" class="msg assistant">
          <div class="msg-bubble">
            <b>AI：</b><span style="white-space: pre-line;">{{ removeMarkdownBold(streamingContent) }}</span>
          </div>
        </div>
      </div>
      <div class="chat-input-container compact">
        <div class="chat-input-bar-centered">
          <el-input v-model="input" :disabled="streaming" placeholder="请输入你的问题..." clearable @keyup.enter.native="sendMessage" style="width: 400px; margin-right: 12px;" />
          <el-button type="primary" :disabled="!input || streaming || !currentSessionId" @click="sendMessage" style="margin-right: 12px;">发送</el-button>
          <el-button type="success" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <!-- MCP工具多选下拉 -->
          <el-select
            v-model="selectedTools"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择工具（可多选）"
            style="min-width: 220px; max-width: 320px; margin-left: 16px; border-radius: 8px;"
            filterable
            clearable
            tag-type="success"
          >
            <el-option
              v-for="tool in mcpTools"
              :key="tool.name"
              :label="tool.description"
              :value="tool.name"
            >
              <el-icon style="margin-right: 6px;"><Tools /></el-icon>
              {{ tool.description }}
            </el-option>
          </el-select>
        </div>
        <!-- 上传文件对话框 -->
        <el-dialog v-model="showUploadDialog" title="上传文件" width="500px">
          <div class="upload-area">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :on-change="onFileChange"
              :file-list="fileList"
              drag
              accept=".txt,.pdf,.doc,.docx"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 txt、pdf、doc、docx 格式文件
                </div>
              </template>
            </el-upload>
          </div>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="showUploadDialog = false">取消</el-button>
              <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile">
                上传
              </el-button>
            </span>
          </template>
        </el-dialog>
      </div>
      <div class="upload-tip">支持上传文件</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ChatLineRound, Upload, UploadFilled, Tools } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import axios from 'axios'

const input = ref('')
const messages = ref([])
const streaming = ref(false)
const streamingContent = ref('')
const sessions = ref([])
const currentSessionId = ref(null)
const editSessionId = ref(null)
const editSessionTitle = ref('')
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const uploading = ref(false)
const uploadRef = ref()

// MCP工具相关
const mcpTools = ref([]);
const selectedTools = ref([]);

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleString()
}

async function fetchSessions() {
  const token = localStorage.getItem('token')
  const res = await fetch('/api/chat/sessions', { headers: { Authorization: token } })
  sessions.value = await res.json()
  if (sessions.value.length && !currentSessionId.value) {
    switchSession(sessions.value[0].id)
  }
}

async function switchSession(id) {
  currentSessionId.value = id
  await fetchMessages()
}

async function fetchMessages() {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/chat/session/${currentSessionId.value}`, { headers: { Authorization: token } })
  const rawMsgs = await res.json()
  // 解析 reference 字段
  messages.value = rawMsgs.map(msg => {
    let reference = []
    try {
      if (msg.reference) reference = JSON.parse(msg.reference)
    } catch (e) {}
    return { ...msg, reference, referenceCollapsed: false }
  })
}

async function createSession() {
  const token = localStorage.getItem('token')
  const res = await fetch('/api/chat/session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({ title: '新会话' })
  })
  const data = await res.json()
  if (data.success) {
    await fetchSessions()
    currentSessionId.value = data.id
    messages.value = []
  } else {
    ElMessage.error(data.msg || '新建会话失败')
  }
}

// 只保留换行和去除**的处理
function removeMarkdownBold(content) {
  return content.replace(/\*\*/g, '').replace(/(\d+\.)/g, '\n$1');
}

async function sendMessage() {
  if (!input.value.trim() || !currentSessionId.value) return
  streamingContent.value = ''
  messages.value.push({ role: 'user', content: input.value })
  streaming.value = true
  const prompt = input.value
  input.value = ''

  try {
    // 1. 先保存用户消息到数据库
    const token = localStorage.getItem('token')
    await fetch(`/api/chat/session/${currentSessionId.value}/message`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: token },
      body: JSON.stringify({ role: 'user', content: prompt })
    })

    // 2. 先获取AI参考内容
    const refRes = await fetch(`/api/chat/session/${currentSessionId.value}/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: prompt
    })
    const thisReference = await refRes.json()
    console.log('thisReference', thisReference)
    // 强制让 reference 字段始终有内容
    let fixedReference = thisReference
    if (!Array.isArray(thisReference) || thisReference.length === 0) {
      fixedReference = [{ source: 'global', text: '【无检索结果，已兜底，后端返回空数组】' }]
    }

    // 3. 流式请求AI回复
    console.log('发送的工具选择:', selectedTools.value)
    const requestBody = {
      prompt: prompt,
      tools: selectedTools.value
    }
    console.log('请求体:', requestBody)
    const res = await fetch(`/api/chat/session/${currentSessionId.value}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify(requestBody)
    })
    if (!res.body) throw new Error('无响应流')
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let done = false
    let buffer = ''
    streamingContent.value = ''
    while (!done) {
      const { value, done: doneReading } = await reader.read()
      done = doneReading
      if (value) {
        buffer += decoder.decode(value, { stream: true })
        let lines = buffer.split(/\r?\n/)
        buffer = lines.pop()
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.replace('data:', '').trim()
            if (data) streamingContent.value += data
          }
        }
      }
    }
    // 4. AI回复结束后，重新获取消息列表以包含MCP内容
    await fetchMessages()
    streaming.value = false
    streamingContent.value = ''
  } catch (e) {
    streaming.value = false
    streamingContent.value = ''
    ElMessage.error('AI回复失败')
  }
}

function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!currentSessionId.value) {
    ElMessage.warning('请先选择或新建会话')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const token = localStorage.getItem('token')
    // 调试用，打印关键信息
    console.log('token:', token)
    console.log('sessionId:', currentSessionId.value)
    console.log('file:', selectedFile.value)
    const res = await axios.post(`/api/chat/session/${currentSessionId.value}/upload`, formData, {
      headers: {
        Authorization: token
      }
    })
    if (res.status === 200) {
      ElNotification.success({
        title: '上传成功',
        message: '文件上传成功！本对话知识已更新，下次提问可直接用新文件内容',
        duration: 3000
      })
      showUploadDialog.value = false
      selectedFile.value = null
      fileList.value = []
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    } else {
      ElMessage.error('上传失败：' + (res.data || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data || e.message))
  } finally {
    uploading.value = false
  }
}

function startEditSession(session) {
  editSessionId.value = session.id
  editSessionTitle.value = session.title
}

async function saveSessionTitle(session) {
  if (!editSessionTitle.value.trim() || editSessionTitle.value === session.title) {
    editSessionId.value = null
    return
  }
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/chat/session/${session.id}/rename`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({ title: editSessionTitle.value })
  })
  const data = await res.json()
  if (data.success) {
    session.title = editSessionTitle.value
    ElMessage.success('重命名成功')
    await fetchSessions()
  } else {
    ElMessage.error(data.msg || '重命名失败')
  }
  editSessionId.value = null
}

async function deleteSession(id) {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/chat/session/${id}`, {
    method: 'DELETE',
    headers: { Authorization: token }
  })
  const data = await res.json()
  if (data.success) {
    ElMessage.success('删除成功')
    await fetchSessions()
    if (currentSessionId.value === id) {
      if (sessions.value.length) {
        currentSessionId.value = sessions.value[0].id
        await fetchMessages()
      } else {
        currentSessionId.value = null
        messages.value = []
      }
    }
  } else {
    ElMessage.error(data.msg || '删除失败')
  }
}

onMounted(() => {
  fetchSessions()
  fetchMcpTools()
})

async function fetchMcpTools() {
  try {
    const res = await fetch('/api/mcp/tools');
    const allTools = await res.json();
    // 只保留本地工具
    mcpTools.value = allTools.filter(tool => [
      'time-mcp',
      'random-mcp',
      'filesystem-mcp',
      'unitconvert-mcp',
      'mysql-mcp'
    ].includes(tool.name));
  } catch (e) {
    mcpTools.value = [];
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  background: #f5f6fa;
}
.chat-sessions {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  padding: 0;
  flex-shrink: 0;
}
.session-header {
  display: flex;
  align-items: center;
  padding: 18px 16px 10px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8fafc;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.session-item {
  position: relative;
  padding: 14px 18px 10px 18px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background 0.2s, border-color 0.2s;
  display: flex;
  flex-direction: column;
  background: #fff;
  margin-bottom: 6px;
  border-radius: 8px;
}
.session-item.active {
  background: #e3f2fd;
  border-left: 3px solid #1976d2;
}
.session-item-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.session-title {
  font-size: 1rem;
  font-weight: 500;
  color: #1976d2;
  margin-bottom: 2px;
  word-break: break-all;
}
.session-time {
  font-size: 12px;
  color: #90a4ae;
}
.chat-page {
  flex: 1 1 0%;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6fa;
  min-width: 0;
  max-width: 100vw;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  flex-shrink: 0;
  z-index: 10;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 100px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f4f6fa;
}

.chat-input-container.compact {
  padding: 8px 24px;
  box-shadow: 0 -1px 4px rgba(0,0,0,0.04);
  width: 100%;
  background: #fff;
}
.chat-input-bar-centered {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 0 auto;
  padding: 16px 0;
}
.chat-input.compact {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  gap: 8px;
  padding: 4px 0;
  border-radius: 8px;
  box-shadow: none;
  margin: 0;
}
.chat-input.compact .el-form-item {
  margin-bottom: 0;
}
.chat-input.compact .el-form-item.input {
  flex: 1 1 0%;
  min-width: 0;
}
.chat-input.compact .el-form-item.button,
.chat-input.compact .el-form-item.upload {
  flex: none;
}
.chat-input.compact .el-input__wrapper {
  min-height: 32px;
  width: 100%;
}
.chat-input.compact .el-button {
  height: 32px;
  font-size: 15px;
  padding: 0 14px;
  min-width: 64px;
}

.msg {
  display: flex;
  justify-content: flex-start;
}

.msg.user {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 80%;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 1.08rem;
  line-height: 1.7;
  background: #e3f2fd;
  color: #1976d2;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.06);
  word-break: break-word;
  white-space: pre-line;
}

.msg.user .msg-bubble {
  background: #1976d2;
  color: #fff;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.12);
}

.msg.assistant .msg-bubble {
  background: #e3f2fd;
  color: #1976d2;
}

.cursor {
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.upload-demo .el-button {
  margin-left: 0;
  border-radius: 50%;
  height: 40px;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
  background: #f4f6fa;
  border: 1px solid #dbeafe;
  transition: background 0.2s;
}

.upload-demo .el-button:hover {
  background: #e3f2fd;
  border-color: #90caf9;
}

.upload-tip {
  text-align: right;
  color: #90a4ae;
  font-size: 13px;
  margin-top: 4px;
  margin-right: 8px;
}

.session-action {
  color: #90a4ae;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
}

.session-action:hover {
  color: #1976d2;
}

.session-x {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #e57373;
  user-select: none;
  transition: color 0.2s;
  z-index: 2;
}

.session-x:hover {
  color: #d32f2f;
}

.upload-area {
  padding: 20px 0;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.reference-bar {
  background: #f4f8fb;
  color: #1976d2;
  font-size: 14px;
  padding: 8px 18px;
  border-radius: 8px;
  margin-bottom: 10px;
}
.ref-item {
  margin-right: 8px;
}
.ref-global {
  color: #888;
}
.ref-session {
  color: #888;
}

/* MCP工具调用内容样式 */
.mcp-content {
  margin-top: 12px;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f8faff;
  overflow: hidden;
}

.mcp-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 14px;
  font-weight: 500;
}

.mcp-body {
  padding: 12px 16px;
  background: #f8faff;
}

.mcp-body pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}
</style> 