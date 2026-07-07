<template>
  <div>
    <div class="page-header">
      <h3>黄金链路拓扑</h3>
      <div v-if="canEdit" class="header-actions">
        <el-button type="primary" size="small" @click="editingLink = null; showEditorDialog = true">
          <el-icon><Plus /></el-icon> 新增连接
        </el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <div v-if="links.length" ref="chartRef" style="width:100%;height:500px"></div>
      <el-empty v-else description="暂无拓扑配置，请在编辑页面导入" />
    </el-card>

    <!-- 详情面板 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" direction="rtl" size="480px" @close="handleDrawerClose">
      <!-- 节点视图：列出该节点的所有关联连接 -->
      <template v-if="viewMode === 'node' && selectedNode">
        <div style="margin-bottom:12px">
          <el-tag type="info">节点：{{ selectedNode }}</el-tag>
        </div>

        <template v-if="outgoingLinks.length">
          <h4 style="margin:0 0 8px;color:#409EFF">出边 ({{ outgoingLinks.length }})</h4>
          <div v-for="link in outgoingLinks" :key="link.linkId" class="connection-item" @click="switchToEdgeView(link)">
            <div class="connection-row">
              <span class="conn-from">{{ link.fromNode }}</span>
              <el-icon><ArrowRight /></el-icon>
              <span class="conn-to">{{ link.toNode }}</span>
            </div>
            <div class="conn-meta">
              <el-tag size="small" :color="protocolColor(link.protocol)" style="color:#fff;border:0">
                {{ link.protocol || '未知' }}
              </el-tag>
              <span v-if="link.interfaceName" style="margin-left:8px;color:#606266">
                {{ link.interfaceName }}
              </span>
            </div>
          </div>
        </template>

        <template v-if="incomingLinks.length">
          <h4 v-if="outgoingLinks.length" style="margin:16px 0 8px;color:#E6A23C">入边 ({{ incomingLinks.length }})</h4>
          <h4 v-else style="margin:0 0 8px;color:#E6A23C">入边 ({{ incomingLinks.length }})</h4>
          <div v-for="link in incomingLinks" :key="link.linkId" class="connection-item" @click="switchToEdgeView(link)">
            <div class="connection-row">
              <span class="conn-from">{{ link.fromNode }}</span>
              <el-icon><ArrowRight /></el-icon>
              <span class="conn-to">{{ link.toNode }}</span>
            </div>
            <div class="conn-meta">
              <el-tag size="small" :color="protocolColor(link.protocol)" style="color:#fff;border:0">
                {{ link.protocol || '未知' }}
              </el-tag>
              <span v-if="link.interfaceName" style="margin-left:8px;color:#606266">
                {{ link.interfaceName }}
              </span>
            </div>
          </div>
        </template>

        <el-empty v-if="!outgoingLinks.length && !incomingLinks.length" description="该节点没有关联连接" />
      </template>

      <!-- 连线视图：展示选中连线的详细信息 -->
      <template v-else-if="viewMode === 'edge' && selectedLink">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="起始节点">{{ selectedLink.fromNode }}</el-descriptions-item>
              <el-descriptions-item label="目标节点">{{ selectedLink.toNode }}</el-descriptions-item>
              <el-descriptions-item label="通信协议">
                <el-tag size="small" :color="protocolColor(selectedLink.protocol)" style="color:#fff;border:0">
                  {{ selectedLink.protocol || '-' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="接口名称">{{ selectedLink.interfaceName || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div v-if="canEdit" style="margin-top:16px;text-align:right">
              <el-button type="primary" size="small" @click="openEditor(selectedLink)">编辑</el-button>
              <el-popconfirm title="确定删除该连接？" @confirm="handleDeleteLink(selectedLink.linkId)">
                <template #reference>
                  <el-button type="danger" size="small">删除连接</el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-tab-pane>
          <el-tab-pane label="接口详情" name="details" v-if="selectedLink.interfaceDetails">
            <div style="padding:8px;background:#fafafa;border-radius:4px">
              <MarkdownViewer :content="selectedLink.interfaceDetails" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 编辑/新增连接弹窗 -->
    <TopologyEditorDialog
      v-model="showEditorDialog"
      :link="editingLink"
      ref="editorRef"
      @save="handleEditorSave"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTopologyList, batchSaveTopology, deleteTopology } from '@/api/topology'
import { usePermission } from '@/composables/usePermission'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'
import TopologyEditorDialog from '@/components/topology/TopologyEditorDialog.vue'
import type { TopologyLink } from '@/types/topology'
import * as echarts from 'echarts'
import { Plus, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const links = ref<TopologyLink[]>([])
const loading = ref(false)
const chartRef = ref<HTMLElement>()
const chart = ref<echarts.ECharts | null>(null)
let handleResize: (() => void) | null = null

// 权限
const { fetchPermission, canEdit: canEditPerm } = usePermission()
const canEdit = computed(() => canEditPerm(systemId).value)

// 详情面板
const drawerVisible = ref(false)
const drawerTitle = ref('')
const viewMode = ref<'edge' | 'node'>('edge')
const selectedLink = ref<TopologyLink | null>(null)
const selectedNode = ref<string | null>(null)
const activeTab = ref('basic')

// 节点视图：出入边列表
const outgoingLinks = computed(() =>
  links.value.filter(l => l.fromNode === selectedNode.value))
const incomingLinks = computed(() =>
  links.value.filter(l => l.toNode === selectedNode.value))

// 编辑弹窗
const showEditorDialog = ref(false)
const editingLink = ref<TopologyLink | null>(null)
const editorRef = ref<InstanceType<typeof TopologyEditorDialog>>()

// 协议颜色映射
const PROTOCOL_COLORS: Record<string, string> = {
  HTTP: '#409EFF',
  HTTPS: '#337ECC',
  TCP: '#E6A23C',
  RPC: '#9B59B6',
  MQ: '#67C23A'
}
const DEFAULT_COLOR = '#909399'

function protocolColor(protocol: string): string {
  return PROTOCOL_COLORS[protocol?.toUpperCase()] || DEFAULT_COLOR
}

  onMounted(async () => {
  loading.value = true
  try {
    await fetchPermission(systemId.value)
    const res = await getTopologyList(systemId.value)
    links.value = res.data || []
    if (links.value.length) {
      await nextTick()
      renderChart()
    }
  } catch { /* empty */ }
  loading.value = false
})

function buildChartOption(): echarts.EChartsOption {
  const nodeSet = new Set<string>()
  links.value.forEach(l => { nodeSet.add(l.fromNode); nodeSet.add(l.toNode) })
  const nodes = Array.from(nodeSet).map(n => ({ name: n, symbolSize: 40 }))

  const protocolTypes = new Set<string>()
  links.value.forEach(l => {
    if (l.protocol) protocolTypes.add(l.protocol.toUpperCase())
  })

  const edges = links.value.map(l => ({
    source: l.fromNode,
    target: l.toNode,
    protocol: l.protocol || '',
    lineStyle: { color: protocolColor(l.protocol), width: 2 },
    label: {
      show: !!(l.protocol || l.interfaceName),
      formatter: l.protocol || l.interfaceName || '',
      fontSize: 10
    },
    value: l.interfaceName || ''
  }))

  return {
    legend: {
      data: Array.from(protocolTypes).filter(Boolean).map(p => ({
        name: p, itemStyle: { color: protocolColor(p) }
      })),
      bottom: 0, left: 'center', icon: 'circle',
      textStyle: { fontSize: 12 }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') return `<b>${params.name}</b>`
        if (params.dataType === 'edge') {
          const p = params.data.protocol || ''
          const iface = params.data.value || ''
          return `<b>${params.data.source} → ${params.data.target}</b><br/>` +
            (p ? `协议: ${p}` : '') +
            (iface ? `${p ? '<br/>' : ''}接口: ${iface}` : '')
        }
        return ''
      }
    },
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      force: { repulsion: 200, edgeLength: 150 },
      data: nodes, links: edges,
      label: { show: true, position: 'bottom' },
      lineStyle: { curveness: 0.2, width: 2 },
      emphasis: { lineStyle: { width: 4, opacity: 0.9 } },
      focusNodeAdjacency: false
    }]
  }
}

function renderChart() {
  if (!chartRef.value) return
  chart.value = echarts.init(chartRef.value)
  chart.value.setOption(buildChartOption())

  chart.value.on('click', (params: any) => {
    if (params.dataType === 'node') {
      selectedNode.value = params.name
      viewMode.value = 'node'
      drawerTitle.value = `节点：${params.name}`
      drawerVisible.value = true
    }
    if (params.dataType === 'edge') {
      const link = links.value.find(l =>
        l.fromNode === params.data.source && l.toNode === params.data.target)
      if (link) switchToEdgeView(link)
    }
  })

  handleResize = () => chart.value?.resize()
  window.addEventListener('resize', handleResize)
}

// 增量刷新图表（不销毁 ECharts 实例）
async function refreshChart() {
  if (!chart.value) {
    await nextTick()
    renderChart()
    return
  }
  chart.value.setOption(buildChartOption(), { notMerge: false })
}

function switchToEdgeView(link: TopologyLink) {
  selectedLink.value = link
  viewMode.value = 'edge'
  activeTab.value = 'basic'
  drawerTitle.value = `${link.fromNode} → ${link.toNode}`
  drawerVisible.value = true
}

async function handleEditorSave(data: {
  fromNode: string; toNode: string; protocol: string
  interfaceName: string; interfaceDetails: string
}) {
  editorRef.value?.setSaving(true)
  try {
    const updatedLinks = links.value.map(l => ({
      fromNode: l.fromNode, toNode: l.toNode,
      protocol: l.protocol, interfaceName: l.interfaceName,
      interfaceDetails: l.interfaceDetails
    }))

    if (!editingLink.value) {
      // 新增
      updatedLinks.push(data)
    } else {
      // 编辑：替换已有连接
      const idx = updatedLinks.findIndex(_ =>
        _.fromNode === editingLink.value!.fromNode && _.toNode === editingLink.value!.toNode
      )
      if (idx >= 0) {
        updatedLinks[idx] = data
      }
    }

    const res = await batchSaveTopology(systemId.value, updatedLinks)
    links.value = res.data || []
    showEditorDialog.value = false
    editingLink.value = null
    ElMessage.success(editingLink.value ? '连接已更新' : '连接添加成功')
    refreshChart()
  } catch { /* handled */ }
  editorRef.value?.setSaving(false)
}

async function handleDeleteLink(linkId: string) {
  try {
    await deleteTopology(systemId.value, linkId)
    links.value = links.value.filter(l => l.linkId !== linkId)
    drawerVisible.value = false
    ElMessage.success('连接已删除')
    refreshChart()
  } catch { /* handled */ }
}

// 关闭抽屉时重置视图状态
function handleDrawerClose() {
  viewMode.value = 'edge'
  selectedNode.value = null
  selectedLink.value = null
}

onUnmounted(() => {
  if (handleResize) {
    window.removeEventListener('resize', handleResize)
    handleResize = null
  }
  chart.value?.dispose()
  chart.value = null
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.connection-item {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.connection-item:hover {
  background: #f5f7fa;
  border-color: #409EFF;
}
.connection-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.conn-from, .conn-to {
  font-weight: 500;
  color: #303133;
}
.conn-meta {
  display: flex;
  align-items: center;
  margin-top: 6px;
}
</style>
