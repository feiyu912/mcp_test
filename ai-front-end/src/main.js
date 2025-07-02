import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import ChatPage from './pages/ChatPage.vue'
import UploadPage from './pages/UploadPage.vue'
import KnowledgeBasePage from './pages/KnowledgeBasePage.vue'

const routes = [
  { path: '/', component: ChatPage, name: 'Chat' },
  { path: '/upload', component: UploadPage, name: 'Upload' },
  { path: '/knowledge-base', component: KnowledgeBasePage, name: 'KnowledgeBase' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

createApp(App).use(router).use(ElementPlus).mount('#app')
