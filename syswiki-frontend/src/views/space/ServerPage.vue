<template>
  <div>
    <div class="page-header">
      <h3>服务器与数据库配置</h3>
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="基础设施" name="SERVER">
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
              <MarkdownViewer v-if="serverContent" :content="serverContent" />
              <el-empty v-else description="暂无基础设施配置内容" />
            </template>
          </el-skeleton>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="网络策略" name="NETWORK">
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
              <MarkdownViewer v-if="networkContent" :content="networkContent" />
              <el-empty v-else description="暂无网络策略内容" />
            </template>
          </el-skeleton>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="数据库" name="DATABASE">
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
              <MarkdownViewer v-if="dbContent" :content="dbContent" />
              <el-empty v-else description="暂无数据库配置内容" />
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
