<script setup>
// 无需引入原有组件，全部由路由切换
</script>

<template>
  <el-container style="min-height: 100vh;">
    <el-aside width="200px" style="background: #e3f2fd; color: #1976d2;">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; font-weight: bold; letter-spacing: 2px; color: #1976d2;">AI</div>
      <el-menu :default-active="$route.path" router background-color="#e3f2fd" text-color="#1976d2" active-text-color="#1976d2" style="border-right: none;">
        <el-menu-item index="/">对话</el-menu-item>
        <el-menu-item index="/knowledge-base">知识库</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header v-if="$route.meta.title" style="background: #f8fafc; box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03); height: 60px; display: flex; align-items: center; padding-left: 24px; font-size: 1.1rem; font-weight: 500; color: #1976d2;">{{$route.meta.title}}</el-header>
      <el-main style="background: #f5f6fa; padding: 0; min-height: calc(100vh - 60px);">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
html, body, #app, .el-container, .el-main {
  height: 100%;
  min-height: 100%;
  padding: 0;
  margin: 0;
}
.el-aside {
  box-shadow: 2px 0 8px 0 rgba(25,118,210,0.04);
}
</style>
