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
      <div class="header-right" style="display:flex;align-items:center;gap:12px">
        <el-tag v-if="authStore.isAdmin" type="danger" size="small">管理员</el-tag>
        <el-tag v-else-if="authStore.isEditor" type="primary" size="small">编辑者</el-tag>
        <el-tag v-else type="info" size="small">只读</el-tag>
        <el-dropdown @command="handleCommand">
          <span style="cursor:pointer;display:flex;align-items:center;gap:4px">
            <el-icon><User /></el-icon>
            {{ authStore.displayName }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="authStore.isAdmin" command="users">用户管理</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useSpaceStore } from '@/stores/space'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const spaceStore = useSpaceStore()
const authStore = useAuthStore()

const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (cmd === 'users') {
    router.push('/admin/users')
  }
}
</script>

<style scoped>
.app-layout { min-height: 100vh; }
.app-header {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed; background: #fff; padding: 0 20px;
}
.app-main { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 60px); }
</style>
