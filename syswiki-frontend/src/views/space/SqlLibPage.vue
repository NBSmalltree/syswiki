<template>
  <div>
    <div class="page-header">
      <h3>运维SQL与指令库</h3>
      <el-button type="primary" link @click="goEdit">管理SQL</el-button>
    </div>

    <!-- 分类筛选 -->
    <div style="margin-bottom:16px">
      <el-radio-group v-model="category" @change="loadData">
        <el-radio-button label="ALL">全部</el-radio-button>
        <el-radio-button label="QUERY">常用查询</el-radio-button>
        <el-radio-button label="CHECK">数据校对</el-radio-button>
        <el-radio-button label="FIX">应急冲正</el-radio-button>
        <el-radio-button label="PERF">性能监控</el-radio-button>
        <el-radio-button label="SHELL">Shell指令</el-radio-button>
      </el-radio-group>
    </div>

    <!-- SQL列表 -->
    <div v-loading="loading">
      <el-card v-for="sql in sqlList" :key="sql.sqlId" style="margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <h4 style="margin:0">{{ sql.title }}</h4>
          <el-tag size="small">{{ sql.category }}</el-tag>
        </div>
        <pre style="background:#f5f5f5;padding:10px;border-radius:4px;font-size:13px;overflow-x:auto"><code>{{ sql.sqlTemplate }}</code></pre>

        <!-- 参数输入 -->
        <div v-if="sql.params && sql.params.length" style="margin-top:10px;display:flex;flex-wrap:wrap;gap:8px;align-items:center">
          <el-input v-for="p in sql.params" :key="p.name" v-model="paramValues[sql.sqlId + '_' + p.name]"
                    :placeholder="p.label || p.name" size="small" style="width:180px" />
          <el-button type="primary" size="small" @click="handleRender(sql)">生成SQL</el-button>
        </div>

        <div style="margin-top:8px">
          <CopyButton :text="renderedSql[sql.sqlId] || sql.sqlTemplate" />
        </div>

        <!-- 渲染结果 -->
        <div v-if="renderedSql[sql.sqlId]" style="margin-top:10px">
          <el-divider />
          <pre style="background:#e8f5e9;padding:10px;border-radius:4px;font-size:13px"><code>{{ renderedSql[sql.sqlId] }}</code></pre>
          <CopyButton :text="renderedSql[sql.sqlId]" label="复制完整SQL" />
        </div>
      </el-card>
      <el-empty v-if="!loading && sqlList.length === 0" description="暂无SQL条目" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSqlList, renderSql } from '@/api/sqlLib'
import CopyButton from '@/components/common/CopyButton.vue'

const route = useRoute()
const router = useRouter()
const systemId = computed(() => route.params.systemId as string)
const category = ref('ALL')
const sqlList = ref<any[]>([])
const loading = ref(false)
const paramValues = reactive<Record<string, string>>({})
const renderedSql = reactive<Record<string, string>>({})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSqlList(systemId.value, category.value)
    sqlList.value = res.data || []
  } catch { /* handled */ }
  loading.value = false
}

const handleRender = async (sql: any) => {
  const params: Record<string, string> = {}
  if (sql.params) {
    for (const p of sql.params) {
      const val = paramValues[sql.sqlId + '_' + p.name]
      if (val) params[p.name] = val
    }
  }
  try {
    const res = await renderSql(systemId.value, sql.sqlId, params)
    renderedSql[sql.sqlId] = res.data?.renderedSql || ''
    ElMessage.success('SQL已生成')
  } catch { /* handled */ }
}

const goEdit = () => router.push(`/space/${systemId.value}/edit/SQL`)

onMounted(loadData)
</script>
