<template>
  <div>
    <div class="page-header">
      <h3>环境架构</h3>
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="测试环境" name="ARCH_TEST">
        <el-card>
          <el-skeleton :loading="loading" animated>
            <template #template>
              <el-skeleton-item variant="h3" style="width:40%" />
              <el-skeleton-item variant="p" style="width:100%" />
              <el-skeleton-item variant="p" style="width:80%" />
              <el-skeleton-item variant="p" style="width:60%" />
              <el-skeleton-item variant="text" style="width:35%; margin-top:16px" />
              <el-skeleton-item variant="p" style="width:90%" />
              <el-skeleton-item variant="p" style="width:100%" />
              <el-skeleton-item variant="p" style="width:70%" />
            </template>
            <template #default>
              <MarkdownViewer v-if="testContent" :content="testContent" />
              <el-empty v-else description="暂无测试环境架构内容" />
            </template>
          </el-skeleton>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="生产环境" name="ARCH_PROD">
        <el-card>
          <el-skeleton :loading="loading" animated>
            <template #template>
              <el-skeleton-item variant="h3" style="width:40%" />
              <el-skeleton-item variant="p" style="width:100%" />
              <el-skeleton-item variant="p" style="width:80%" />
              <el-skeleton-item variant="p" style="width:60%" />
              <el-skeleton-item variant="text" style="width:35%; margin-top:16px" />
              <el-skeleton-item variant="p" style="width:90%" />
              <el-skeleton-item variant="p" style="width:100%" />
              <el-skeleton-item variant="p" style="width:70%" />
            </template>
            <template #default>
              <MarkdownViewer v-if="prodContent" :content="prodContent" />
              <el-empty v-else description="暂无生产环境架构内容" />
            </template>
          </el-skeleton>
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
  const MIN_LOADING_MS = 300
  const start = Date.now()
  loading.value = true
  try {
    const [t, p] = await Promise.all([
      getModuleContent(systemId.value, 'ARCH_TEST'),
      getModuleContent(systemId.value, 'ARCH_PROD')
    ])
    testContent.value = t.data?.mdContent || ''
    prodContent.value = p.data?.mdContent || ''
  } catch { /* empty */ }
  const elapsed = Date.now() - start
  if (elapsed < MIN_LOADING_MS) {
    await new Promise(r => setTimeout(r, MIN_LOADING_MS - elapsed))
  }
  loading.value = false
})
</script>
