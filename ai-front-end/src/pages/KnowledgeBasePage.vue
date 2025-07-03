<template>
  <div class="kb-page">
    <div class="kb-search-bar">
      <el-input v-model="query" placeholder="请输入要检索的内容..." clearable @keyup.enter.native="searchKb" style="width: 400px;" />
      <el-button type="primary" @click="searchKb">搜索</el-button>
      <el-button type="success" @click="showUploadDialog = true">
        <el-icon><Upload /></el-icon>
        上传文件
      </el-button>
    </div>
    
    <!-- 上传文件对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文件到知识库" width="500px">
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
    <div class="kb-result-list">
      <el-empty v-if="!loading && results.length === 0 && searched" description="未找到相关内容" />
      <el-skeleton v-if="loading" rows="4" animated />
      <el-card v-for="(item, idx) in results" :key="idx" class="kb-result-item">
        <div>{{ item.text }}</div>
        <div class="kb-score">相关度：{{ (item.score * 100).toFixed(2) }}%</div>
      </el-card>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'

const query = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)

// 上传相关状态
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const uploading = ref(false)
const uploadRef = ref()

async function searchKb() {
  if (!query.value.trim()) {
    ElMessage.warning('请输入检索内容')
    return
  }
  loading.value = true
  searched.value = true
  results.value = []
  try {
    const res = await fetch('/api/knowledge-base/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain'
      },
      body: query.value
    })
    const data = await res.json()
    results.value = data || []
  } catch (e) {
    ElMessage.error('检索失败')
  } finally {
    loading.value = false
  }
}

// 文件选择处理
function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

// 上传文件处理
async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    
    const res = await fetch('/api/etl/upload', {
      method: 'POST',
      body: formData
    })
    
    if (res.ok) {
      const text = await res.text()
      ElMessage.success('文件上传成功！')
      showUploadDialog.value = false
      // 清空选择
      selectedFile.value = null
      fileList.value = []
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    } else {
      const errorText = await res.text()
      ElMessage.error('上传失败：' + errorText)
    }
  } catch (e) {
    ElMessage.error('上传失败：' + e.message)
  } finally {
    uploading.value = false
  }
}
</script>
<style scoped>
.kb-page {
  max-width: 800px;
  margin: 32px auto;
  padding: 24px 0;
}
.kb-search-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  gap: 12px;
}
.kb-result-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.kb-result-item {
  font-size: 1.08rem;
  line-height: 1.7;
  background: #f8fafc;
  border-radius: 10px;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.04);
  padding: 18px 22px;
}
.kb-score {
  color: #90a4ae;
  font-size: 13px;
  margin-top: 6px;
}

.upload-area {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 