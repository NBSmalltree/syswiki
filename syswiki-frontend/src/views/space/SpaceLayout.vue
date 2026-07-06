<template>
  <div class="space-layout">
    <el-container style="min-height: calc(100vh - 100px)">
      <el-aside width="220px" class="space-sidebar">
        <el-menu :default-active="activeMenu" :router="true">
          <el-menu-item index="intro">
            <el-icon><Document /></el-icon>
            <span>系统简介</span>
          </el-menu-item>
          <el-menu-item index="arch">
            <el-icon><Platform /></el-icon>
            <span>环境架构</span>
          </el-menu-item>
          <el-menu-item index="server">
            <el-icon><Monitor /></el-icon>
            <span>服务器配置</span>
          </el-menu-item>
          <el-menu-item index="guide">
            <el-icon><Connection /></el-icon>
            <span>接入指南</span>
          </el-menu-item>
          <el-menu-item index="sql-lib">
            <el-icon><Coin /></el-icon>
            <span>运维SQL库</span>
          </el-menu-item>
          <el-menu-item index="topology">
            <el-icon><Share /></el-icon>
            <span>黄金链路</span>
          </el-menu-item>
          <el-menu-item index="ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI问答</span>
          </el-menu-item>
          <template v-if="canEdit">
            <el-divider />
            <el-menu-item index="edit">
              <el-icon><Edit /></el-icon>
              <span>编辑内容</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view v-slot="{ Component }">
          <keep-alive include="AiChatPage">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const spaceStore = useSpaceStore()
const { fetchPermission, canEdit: canEditPerm } = usePermission()
const systemId = computed(() => route.params.systemId as string)
const canEdit = canEditPerm(systemId)

const activeMenu = computed(() => {
  const parts = route.path.split('/')
  return parts[parts.length - 1] || 'intro'
})

onMounted(async () => {
  if (systemId.value) {
    spaceStore.fetchSpaceDetail(systemId.value)
    await fetchPermission(systemId.value)
  }
})
</script>

<style scoped>
.space-sidebar { background: #fff; border-right: 1px solid #e4e7ed; }
</style>
