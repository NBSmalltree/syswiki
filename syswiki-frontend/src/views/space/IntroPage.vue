<template>
  <div>
    <div class="page-header">
      <h3>系统简介</h3>
    </div>
    <el-card>
      <el-skeleton :loading="loading" animated :count="6">
        <template #default>
          <MarkdownViewer v-if="content" :content="content" />
          <el-empty v-else description="暂无系统简介内容" />
        </template>
      </el-skeleton>
    </el-card>
    <el-card v-if="techStack.length" style="margin-top:16px">
      <template #header><span>技术栈</span></template>
      <div style="display:flex;flex-wrap:wrap;gap:8px">
        <el-tag v-for="t in techStack" :key="t" effect="plain">{{ t }}</el-tag>
      </div>
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
const techStack = ref<string[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getModuleContent(systemId.value, 'INTRO')
    content.value = res.data?.mdContent || ''
    // 提取技术栈标签
    const match = content.value.match(/## 技术栈\n([\s\S]*?)(?=\n## |\n$)/)
    if (match) {
      techStack.value = match[1].split('\n')
        .filter(l => l.trim().startsWith('-'))
        .map(l => l.replace(/^-\s*/, '').trim())
    }
  } catch { /* empty */ }
  loading.value = false
})
</script>
