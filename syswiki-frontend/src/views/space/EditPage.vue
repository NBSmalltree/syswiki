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

    <!-- 模块选择 + 版本信息 -->
    <el-card>
      <div style="display:flex;align-items:center;flex-wrap:wrap;gap:16px">
        <el-select v-model="selectedModule" @change="loadContent" style="width:200px">
          <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>

        <div v-if="currentVersion > 0" style="display:flex;align-items:center;gap:16px">
          <el-tag effect="plain" size="small">v{{ currentVersion }}</el-tag>
          <span style="color:#909399;font-size:13px">
            最近更新：{{ currentOperator }} · {{ formatTime(currentUpdateTime) }}
          </span>
          <el-button type="primary" link size="small" @click="showHistory = true">
            <el-icon><Clock /></el-icon> 版本历史
          </el-button>
        </div>
        <span v-else style="color:#c0c4cc;font-size:13px">该模块暂无内容</span>
      </div>
    </el-card>

    <!-- 编辑区 -->
    <div class="editor-area" v-loading="loading">
      <el-input
        v-model="markdownContent"
        type="textarea"
        :autosize="{ minRows: 20, maxRows: 40 }"
        placeholder="在此输入Markdown内容..."
      />
    </div>

    <!-- 实时预览 -->
    <el-card style="margin-top:16px">
      <template #header><span>实时预览</span></template>
      <MarkdownViewer :content="markdownContent" />
    </el-card>

    <!-- 版本历史抽屉 -->
    <el-drawer v-model="showHistory" title="版本历史" direction="rtl" size="500px">
      <el-table :data="versionList" v-loading="historyLoading" stripe size="small">
        <el-table-column label="版本" width="70">
          <template #default="{ row }">
            <el-tag :type="row.version === currentVersion ? 'success' : 'info'" size="small">
              v{{ row.version }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="更新时间">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="previewVersion(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyLoading && versionList.length === 0" description="暂无版本记录" />
    </el-drawer>

    <!-- 历史版本预览弹窗 -->
    <el-dialog v-model="showPreview" :title="`版本 v${previewVersionData?.version} 内容预览`" width="70%" top="5vh">
      <div style="max-height:65vh;overflow-y:auto;padding:12px;background:#fafafa;border-radius:4px">
        <MarkdownViewer :content="previewContent" />
      </div>
      <template #footer>
        <el-button @click="showPreview = false">关闭</el-button>
        <el-button type="primary" @click="restoreVersion">恢复此版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getModuleContent, saveModuleContent, importMarkdown, getVersionHistory } from '@/api/content'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const selectedModule = ref((route.params.moduleType as string) || 'INTRO')
const markdownContent = ref('')
const currentVersion = ref(0)
const currentOperator = ref('')
const currentUpdateTime = ref('')
const loading = ref(false)
const saving = ref(false)

// 版本历史
const showHistory = ref(false)
const historyLoading = ref(false)
const versionList = ref<any[]>([])

// 版本预览
const showPreview = ref(false)
const previewVersionData = ref<any>(null)
const previewContent = ref('')

const moduleOptions = [
  { label: '系统简介与技术栈', value: 'INTRO' },
  { label: '测试环境架构', value: 'ARCH_TEST' },
  { label: '生产环境架构', value: 'ARCH_PROD' },
  { label: '服务器配置', value: 'SERVER' },
  { label: '网络策略', value: 'NETWORK' },
  { label: '数据库配置', value: 'DATABASE' },
  { label: '快速接入指南', value: 'GUIDE' }
]

const formatTime = (t: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const loadContent = async () => {
  loading.value = true
  try {
    const res = await getModuleContent(systemId.value, selectedModule.value)
    if (res.data) {
      markdownContent.value = res.data.mdContent || ''
      currentVersion.value = res.data.version || 0
      currentOperator.value = res.data.operator || ''
      currentUpdateTime.value = res.data.updateTime || res.data.createTime || ''
    } else {
      markdownContent.value = ''
      currentVersion.value = 0
      currentOperator.value = ''
      currentUpdateTime.value = ''
    }
  } catch { markdownContent.value = '' }
  loading.value = false
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveModuleContent(systemId.value, selectedModule.value, {
      mdContent: markdownContent.value,
      operator: '' // 后端自动取当前登录用户
    })
    ElMessage.success('保存成功')
    await loadContent()
  } catch { /* handled */ }
  saving.value = false
}

const handleImport = async (file: File) => {
  try {
    await importMarkdown(systemId.value, file, '')
    ElMessage.success('导入成功')
    await loadContent()
  } catch { /* handled */ }
  return false
}

const handleExport = () => {
  window.open(`/api/spaces/${systemId.value}/contents/export?modules=${selectedModule.value}`)
}

// 加载版本历史
const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getVersionHistory(systemId.value, selectedModule.value)
    versionList.value = res.data || []
  } catch { /* handled */ }
  historyLoading.value = false
}

// 查看历史版本
const previewVersion = (ver: any) => {
  previewVersionData.value = ver
  previewContent.value = ver.mdContent || '（该版本无内容）'
  showPreview.value = true
}

// 恢复历史版本
const restoreVersion = () => {
  if (previewContent.value) {
    markdownContent.value = previewContent.value
    showPreview.value = false
    ElMessage.success('已恢复到 v' + previewVersionData.value.version + '，请保存生效')
  }
}

// 打开历史面板时加载数据
import { watch } from 'vue'
watch(showHistory, (val) => { if (val) loadHistory() })

onMounted(loadContent)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.editor-area { margin-top: 16px; }
</style>
