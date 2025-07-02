<template>
  <div>
    <h2>文件上传</h2>
    <form @submit.prevent="handleUpload">
      <input type="file" @change="onFileChange" />
      <button type="submit" :disabled="uploading">上传</button>
    </form>
    <div v-if="result" :style="{marginTop: '1rem', color: resultColor}">{{ result }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const file = ref(null)
const result = ref('')
const resultColor = ref('black')
const uploading = ref(false)

function onFileChange(e) {
  file.value = e.target.files[0]
}

async function handleUpload() {
  if (!file.value) {
    result.value = '请先选择文件！'
    resultColor.value = 'red'
    return
  }
  uploading.value = true
  result.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    const res = await fetch('/api/etl/upload', {
      method: 'POST',
      body: formData
    })
    const text = await res.text()
    result.value = text
    resultColor.value = res.ok ? 'green' : 'red'
  } catch (e) {
    result.value = '上传失败：' + e.message
    resultColor.value = 'red'
  } finally {
    uploading.value = false
  }
}
</script> 