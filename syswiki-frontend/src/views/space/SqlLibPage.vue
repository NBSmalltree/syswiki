<template>
  <div>
    <div class="page-header">
      <h3>运维SQL与指令库</h3>
      <el-button type="primary" @click="openAdd">新增SQL</el-button>
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
          <div style="display:flex;align-items:center;gap:8px">
            <el-tag size="small">{{ sql.category }}</el-tag>
            <el-button type="danger" size="small" link @click="handleDelete(sql)">删除</el-button>
          </div>
        </div>
        <div v-if="sql.description" style="color:#909399;font-size:13px;margin-bottom:8px">{{ sql.description }}</div>
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
      <el-empty v-if="!loading && sqlList.length === 0" description="暂无SQL条目，点击右上角新增" />
    </div>

    <!-- 新增SQL对话框 -->
    <el-dialog v-model="showAdd" title="新增SQL条目" width="650px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如：根据交易单号查询流水" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="常用查询" value="QUERY" />
            <el-option label="数据校对" value="CHECK" />
            <el-option label="应急冲正" value="FIX" />
            <el-option label="性能监控" value="PERF" />
            <el-option label="Shell指令" value="SHELL" />
          </el-select>
        </el-form-item>
        <el-form-item label="SQL模板" required>
          <el-input v-model="form.sqlTemplate" type="textarea" :rows="5"
                    placeholder="支持参数占位符，如: WHERE bill_no = :billNo" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" placeholder="使用说明（可选）" />
        </el-form-item>
        <el-form-item label="参数定义">
          <div v-for="(p, idx) in form.params" :key="idx" style="display:flex;gap:8px;margin-bottom:8px">
            <el-input v-model="p.name" placeholder="参数名" style="width:150px" />
            <el-input v-model="p.label" placeholder="显示名称" style="width:150px" />
            <el-button type="danger" link @click="form.params.splice(idx, 1)">删除</el-button>
          </div>
          <el-button size="small" @click="form.params.push({ name: '', label: '' })">添加参数</el-button>
          <div style="color:#909399;font-size:12px;margin-top:4px">参数名需与SQL模板中的 :参数名 一致</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSqlList, addSqlItem, deleteSqlItem, renderSql } from '@/api/sqlLib'
import CopyButton from '@/components/common/CopyButton.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const category = ref('ALL')
const sqlList = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showAdd = ref(false)
const paramValues = reactive<Record<string, string>>({})
const renderedSql = reactive<Record<string, string>>({})

const form = ref({
  title: '',
  category: 'QUERY',
  sqlTemplate: '',
  description: '',
  params: [] as { name: string; label: string }[]
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSqlList(systemId.value, category.value)
    sqlList.value = res.data || []
  } catch { /* handled */ }
  loading.value = false
}

const openAdd = () => {
  form.value = { title: '', category: 'QUERY', sqlTemplate: '', description: '', params: [] }
  showAdd.value = true
}

const handleAdd = async () => {
  if (!form.value.title || !form.value.category || !form.value.sqlTemplate) {
    ElMessage.warning('请填写标题、分类和SQL模板')
    return
  }
  saving.value = true
  try {
    await addSqlItem(systemId.value, {
      ...form.value,
      params: form.value.params.filter(p => p.name),
      operator: '当前用户'
    })
    ElMessage.success('新增成功')
    showAdd.value = false
    await loadData()
  } catch { /* handled */ }
  saving.value = false
}

const handleDelete = async (sql: any) => {
  try {
    await ElMessageBox.confirm(`确定删除「${sql.title}」？`, '确认删除', { type: 'warning' })
    await deleteSqlItem(systemId.value, sql.sqlId)
    ElMessage.success('已删除')
    await loadData()
  } catch { /* cancelled */ }
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

onMounted(loadData)
</script>
