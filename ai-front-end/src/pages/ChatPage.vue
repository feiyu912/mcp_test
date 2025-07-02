<template>
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
    
    <div class="chat-input-container">
      <el-form @submit.prevent="sendMessage" class="chat-input" inline>
        <el-form-item style="flex:1; margin-bottom:0;">
          <el-input v-model="input" :disabled="streaming" placeholder="请输入你的问题..." clearable @keyup.enter.native="sendMessage" />
        </el-form-item>
        <el-form-item style="margin-bottom:0;">
          <el-button type="primary" :disabled="!input || streaming" @click="sendMessage">发送</el-button>
        </el-form-item>
        <el-form-item style="margin-bottom:0;">
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
</template>

<script setup>
import { ref } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const input = ref('')
const messages = ref([])
const streaming = ref(false)
const streamingContent = ref('')

// 只保留换行和去除**的处理
function removeMarkdownBold(content) {
  // 去除**，自动在 1. 2. 3. 等序号前加换行
  return content
    .replace(/\*\*/g, '')
    .replace(/(\d+\.)/g, '\n$1');
}

async function sendMessage() {
  if (!input.value.trim()) return
  streamingContent.value = ''
  messages.value.push({ role: 'user', content: input.value })
  streaming.value = true
  const prompt = input.value
  input.value = ''

  try {
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
</script>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  position: relative;
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
  padding: 24px;
  padding-bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f4f6fa;
}

.chat-input-container {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 16px 24px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.1);
  z-index: 100;
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.06);
  padding: 12px 16px;
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
</style> 