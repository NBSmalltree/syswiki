<template>
  <el-container class="app-layout">
    <el-header height="60px" class="app-header">
      <div class="header-left" @click="router.push('/portal')" style="cursor:pointer">
        <h2 style="margin:0;color:#409eff">系统百科</h2>
      </div>
      <div class="header-center">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/portal' }">门户</el-breadcrumb-item>
          <el-breadcrumb-item v-if="spaceStore.currentSpace">
            {{ spaceStore.currentSystemName }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <span>操作人</span>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useSpaceStore } from '@/stores/space'
import { useRouter } from 'vue-router'

const router = useRouter()
const spaceStore = useSpaceStore()
</script>

<style scoped>
.app-layout { min-height: 100vh; }
.app-header {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed; background: #fff; padding: 0 20px;
}
.app-main { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 60px); }
</style>
