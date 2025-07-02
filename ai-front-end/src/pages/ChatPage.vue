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
        <div v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
          <div class="msg-bubble">
            <b>{{ msg.role === 'user' ? '我' : 'AI' }}：</b>
            <span v-if="msg.role === 'assistant'" style="white-space: pre-line;">{{ removeMarkdownBold(msg.content) }}</span>
            <span v-else>{{ msg.content }}</span>
          </div>
        </div>
        <div v-if="streaming" class="msg assistant">
          <div class="msg-bubble">
            <b>AI：</b><span style="white-space: pre-line;">{{ removeMarkdownBold(streamingContent) }}</span>
          </div>
        </div>
      </div>
      <div class="chat-input-container compact">
        <el-form @submit.prevent="sendMessage" class="chat-input compact" inline>
          <el-form-item class="input" style="flex:1; margin-bottom:0;">
            <el-input v-model="input" :disabled="streaming" placeholder="请输入你的问题..." clearable @keyup.enter.native="sendMessage" />
          </el-form-item>
          <el-form-item class="button" style="margin-bottom:0;">
            <el-button type="primary" :disabled="!input || streaming || !currentSessionId" @click="sendMessage">发送</el-button>
          </el-form-item>
          <el-form-item class="upload" style="margin-bottom:0;">
            <el-upload
              class="upload-demo"
              action="/api/knowledge-base/upload"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :disabled="streaming"
            >
              <el-tooltip content="上传文件" placement="top">
                <el-button circle size="large" type="info" plain style="padding: 0 10px; font-weight: 500; letter-spacing: 2px;">
                  上传
                </el-button>
              </el-tooltip>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>
      <div class="upload-tip">支持上传文件</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const input = ref('')
const messages = ref([])
const streaming = ref(false)
const streamingContent = ref('')
const sessions = ref([])
const currentSessionId = ref(null)
const editSessionId = ref(null)
const editSessionTitle = ref('')

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
  messages.value = await res.json()
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

    // 2. 流式请求AI回复
    const res = await fetch('/api/knowledge-base/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain',
        'Accept': 'text/event-stream'
      },
      body: prompt
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
            const data = line.replace(/^data:/, '').trim()
            streamingContent.value += data
          }
        }
      }
    }
    messages.value.push({ role: 'assistant', content: streamingContent.value })
    // 3. 保存AI回复到数据库
    await fetch(`/api/chat/session/${currentSessionId.value}/message`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: token },
      body: JSON.stringify({ role: 'assistant', content: streamingContent.value })
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '对话出错' })
  } finally {
    streaming.value = false
    streamingContent.value = ''
  }
}

function handleUploadSuccess(response, file) {
  ElMessage.success('文件上传成功：' + file.name)
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
})
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
</style> 