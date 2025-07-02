<template>
  <div class="kb-page">
    <div class="kb-search-bar">
      <el-input v-model="query" placeholder="请输入要检索的内容..." clearable @keyup.enter.native="searchKb" style="width: 320px;" />
      <el-button type="primary" @click="searchKb" style="margin-left: 12px;">搜索</el-button>
    </div>
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

const query = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)

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
  margin-bottom: 24px;
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
</style> 