<template>
  <div class="guide-page">
    <div class="page-header">
      <h3>快速接入指南</h3>
      <el-button type="primary" link @click="goEdit">编辑</el-button>
    </div>
    <el-card v-loading="loading">
      <MarkdownViewer v-if="content" :content="content" />
      <el-empty v-else description="暂无接入指南内容" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getModuleContent } from '@/api/content'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const router = useRouter()
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

const goEdit = () => router.push(`/space/${systemId.value}/edit/GUIDE`)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
