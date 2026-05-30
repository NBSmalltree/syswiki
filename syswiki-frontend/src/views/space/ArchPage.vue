<template>
  <div>
    <div class="page-header">
      <h3>环境架构</h3>
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="测试环境" name="ARCH_TEST">
        <el-card v-loading="loading">
          <MarkdownViewer v-if="testContent" :content="testContent" />
          <el-empty v-else description="暂无测试环境架构内容" />
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="生产环境" name="ARCH_PROD">
        <el-card v-loading="loading">
          <MarkdownViewer v-if="prodContent" :content="prodContent" />
          <el-empty v-else description="暂无生产环境架构内容" />
        </el-card>
      </el-tab-pane>
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
const tab = ref('ARCH_TEST')
const testContent = ref('')
const prodContent = ref('')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [t, p] = await Promise.all([
      getModuleContent(systemId.value, 'ARCH_TEST'),
      getModuleContent(systemId.value, 'ARCH_PROD')
    ])
    testContent.value = t.data?.mdContent || ''
    prodContent.value = p.data?.mdContent || ''
  } catch { /* empty */ }
  loading.value = false
})
</script>
