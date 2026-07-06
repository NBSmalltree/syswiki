<template>
  <div class="guide-page">
    <div class="page-header">
      <h3>快速接入指南</h3>
    </div>
    <el-card>
      <el-skeleton :loading="loading" animated :count="6">
        <template #default>
          <MarkdownViewer v-if="content" :content="content" />
          <el-empty v-else description="暂无接入指南内容" />
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getModuleContent } from '@/api/content'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const content = ref('')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getModuleContent(systemId.value, 'GUIDE')
    content.value = res.data?.mdContent || ''
  } catch { /* empty */ }
  loading.value = false
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
