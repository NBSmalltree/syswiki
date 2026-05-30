<template>
  <div>
    <div class="page-header">
      <h3>服务器与数据库配置</h3>
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="基础设施" name="SERVER"><el-card v-loading="loading"><MarkdownViewer v-if="serverContent" :content="serverContent" /><el-empty v-else description="暂无内容" /></el-card></el-tab-pane>
      <el-tab-pane label="网络策略" name="NETWORK"><el-card v-loading="loading"><MarkdownViewer v-if="networkContent" :content="networkContent" /><el-empty v-else description="暂无内容" /></el-card></el-tab-pane>
      <el-tab-pane label="数据库" name="DATABASE"><el-card v-loading="loading"><MarkdownViewer v-if="dbContent" :content="dbContent" /><el-empty v-else description="暂无内容" /></el-card></el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getModuleContent } from '@/api/content'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const tab = ref('SERVER')
const serverContent = ref('')
const networkContent = ref('')
const dbContent = ref('')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [s, n, d] = await Promise.all([
      getModuleContent(systemId.value, 'SERVER'),
      getModuleContent(systemId.value, 'NETWORK'),
      getModuleContent(systemId.value, 'DATABASE')
    ])
    serverContent.value = s.data?.mdContent || ''
    networkContent.value = n.data?.mdContent || ''
    dbContent.value = d.data?.mdContent || ''
  } catch { /* empty */ }
  loading.value = false
})
</script>
