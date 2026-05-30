<template>
  <div>
    <div class="page-header">
      <h3>黄金链路拓扑</h3>
    </div>
    <el-card v-loading="loading">
      <div v-if="links.length" ref="chartRef" style="width:100%;height:500px"></div>
      <el-empty v-else description="暂无拓扑配置，请在编辑页面导入" />
    </el-card>

    <!-- 详情面板 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" direction="rtl" size="450px">
      <div v-if="selectedLink">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="通信协议">{{ selectedLink.protocol }}</el-descriptions-item>
          <el-descriptions-item label="接口名称">{{ selectedLink.interfaceName }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px" v-if="selectedLink.interfaceDetails">
          <h4>接口详情</h4>
          <MarkdownViewer :content="selectedLink.interfaceDetails" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTopologyList } from '@/api/topology'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'
import type { TopologyLink } from '@/types/topology'
import * as echarts from 'echarts'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const links = ref<TopologyLink[]>([])
const loading = ref(false)
const chartRef = ref<HTMLElement>()
const drawerVisible = ref(false)
const selectedLink = ref<TopologyLink | null>(null)
const drawerTitle = ref('')

onMounted(async () => {
  loading.value = true
  try {
    const res = await getTopologyList(systemId.value)
    links.value = res.data || []
    if (links.value.length) {
      await nextTick()
      renderChart()
    }
  } catch { /* empty */ }
  loading.value = false
})

const renderChart = () => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  const nodeSet = new Set<string>()
  links.value.forEach(l => { nodeSet.add(l.fromNode); nodeSet.add(l.toNode) })
  const nodes = Array.from(nodeSet).map(n => ({ name: n, symbolSize: 40 }))
  const edges = links.value.map(l => ({
    source: l.fromNode, target: l.toNode,
    label: { show: true, formatter: l.protocol || '', fontSize: 10 },
    value: l.interfaceName || ''
  }))
  chart.setOption({
    tooltip: {},
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      force: { repulsion: 200, edgeLength: 150 },
      data: nodes, links: edges,
      label: { show: true }, lineStyle: { curveness: 0.2 }
    }]
  })
  chart.on('click', (params: any) => {
    if (params.dataType === 'edge') {
      const link = links.value.find(l =>
        l.fromNode === params.data.source && l.toNode === params.data.target)
      if (link) {
        selectedLink.value = link
        drawerTitle.value = `${link.fromNode} → ${link.toNode}`
        drawerVisible.value = true
      }
    }
  })
}
</script>
