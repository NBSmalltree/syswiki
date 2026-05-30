<template>
  <div class="edit-page">
    <div class="page-header">
      <h3>内容编辑</h3>
      <div class="header-actions">
        <el-upload :show-file-list="false" accept=".md" :before-upload="handleImport">
          <el-button type="success">导入Markdown</el-button>
        </el-upload>
        <el-button @click="handleExport">导出Markdown</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <el-card>
      <el-form label-width="100px">
        <el-form-item label="选择模块">
          <el-select v-model="selectedModule" @change="loadContent">
            <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
          <span v-if="currentVersion" style="margin-left:16px;color:#909399">
            当前版本: v{{ currentVersion }} | 操作人: {{ currentOperator }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="editor-area" v-loading="loading">
      <el-input
        v-model="markdownContent"
        type="textarea"
        :autosize="{ minRows: 20, maxRows: 40 }"
        placeholder="在此输入Markdown内容..."
      />
    </div>

    <el-card style="margin-top:16px">
      <template #header><span>实时预览</span></template>
      <MarkdownViewer :content="markdownContent" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getModuleContent, saveModuleContent, importMarkdown } from '@/api/content'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const selectedModule = ref((route.params.moduleType as string) || 'INTRO')
const markdownContent = ref('')
const currentVersion = ref(0)
const currentOperator = ref('')
const loading = ref(false)
const saving = ref(false)

const moduleOptions = [
  { label: '系统简介与技术栈', value: 'INTRO' },
  { label: '测试环境架构', value: 'ARCH_TEST' },
  { label: '生产环境架构', value: 'ARCH_PROD' },
  { label: '服务器配置', value: 'SERVER' },
  { label: '网络策略', value: 'NETWORK' },
  { label: '数据库配置', value: 'DATABASE' },
  { label: '快速接入指南', value: 'GUIDE' }
]

const loadContent = async () => {
  loading.value = true
  try {
    const res = await getModuleContent(systemId.value, selectedModule.value)
    if (res.data) {
      markdownContent.value = res.data.mdContent || ''
      currentVersion.value = res.data.version || 0
      currentOperator.value = res.data.operator || ''
    } else {
      markdownContent.value = ''
      currentVersion.value = 0
    }
  } catch { markdownContent.value = '' }
  loading.value = false
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveModuleContent(systemId.value, selectedModule.value, {
      mdContent: markdownContent.value,
      operator: '当前用户'
    })
    ElMessage.success('保存成功')
    await loadContent()
  } catch { /* error handled by interceptor */ }
  saving.value = false
}

const handleImport = async (file: File) => {
  try {
    await importMarkdown(systemId.value, file, '当前用户')
    ElMessage.success('导入成功')
    await loadContent()
  } catch { /* handled */ }
  return false // 阻止el-upload默认上传
}

const handleExport = () => {
  window.open(`/api/spaces/${systemId.value}/contents/export?modules=${selectedModule.value}`)
}

onMounted(loadContent)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.editor-area { margin-top: 16px; }
</style>
