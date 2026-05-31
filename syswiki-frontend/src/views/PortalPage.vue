<template>
  <div style="max-width:1200px;margin:0 auto;padding:40px 20px">
    <div style="text-align:center;margin-bottom:40px">
      <h1 style="font-size:32px;color:#303133;margin-bottom:8px">系统百科平台</h1>
      <p style="color:#909399;font-size:16px">统一系统知识管理，打破信息孤岛</p>
    </div>

    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h3>已入驻系统</h3>
      <el-button v-if="authStore.isEditor" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 创建系统空间
      </el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-for="space in spaceList" :key="space.systemId" style="margin-bottom:20px">
        <el-card shadow="hover" style="position:relative">
          <div style="cursor:pointer" @click="enterSpace(space.systemId)">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
              <h4 style="margin:0">{{ space.systemName }}</h4>
              <el-tag size="small">{{ space.systemCode }}</el-tag>
            </div>
            <p v-if="space.description" style="color:#909399;font-size:13px;margin:4px 0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
              {{ space.description }}
            </p>
            <p style="color:#909399;font-size:13px;margin:4px 0">负责人：{{ space.owner }}</p>
          </div>
          <!-- 操作按钮（始终占位，保持卡片等高） -->
          <div style="display:flex;gap:6px;margin-top:10px;border-top:1px solid #f0f0f0;padding-top:10px;min-height:28px"
               @click.stop>
            <el-button v-if="canEdit(space)" type="primary" link size="small" @click="openEdit(space)">编辑</el-button>
            <el-button v-if="authStore.isAdmin" type="danger" link size="small" @click="handleDelete(space)">删除</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24" v-if="!loading && spaceList.length === 0">
        <el-empty description="暂无系统空间，点击上方按钮创建" />
      </el-col>
    </el-row>

    <!-- 创建对话框 -->
    <el-dialog v-model="showCreate" title="创建系统空间" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="系统名称" required>
          <el-input v-model="form.systemName" placeholder="如：超级网上银行" />
        </el-form-item>
        <el-form-item label="系统代号" required>
          <el-input v-model="form.systemCode" placeholder="如：SIB" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="系统简要描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEdit" title="编辑系统信息" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="系统名称" required>
          <el-input v-model="editForm.systemName" />
        </el-form-item>
        <el-form-item label="系统代号">
          <el-input v-model="editForm.systemCode" placeholder="如：SIB" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSpaceList, createSpace, updateSpace, deleteSpace, getSpacePermission } from '@/api/space'
import { useAuthStore } from '@/stores/auth'
import type { Space, CreateSpaceForm } from '@/types/space'

const router = useRouter()
const authStore = useAuthStore()
const spaceList = ref<Space[]>([])
const loading = ref(false)

// 权限缓存 systemId → { canEdit, isAdmin }
const permMap = ref<Record<string, { canEdit: boolean; isAdmin: boolean }>>({})

// 创建
const showCreate = ref(false)
const creating = ref(false)
const form = ref<CreateSpaceForm>({ systemName: '', systemCode: '', owner: '', description: '' })

// 编辑
const showEdit = ref(false)
const editing = ref(false)
const editForm = ref({ systemId: '', systemName: '', systemCode: '', description: '' })

const canEdit = (space: Space) => {
  const p = permMap.value[space.systemId]
  return p?.canEdit || false
}

const loadSpaces = async () => {
  loading.value = true
  try {
    const res = await getSpaceList()
    spaceList.value = res.data || []
    // 并行加载每个系统的权限
    const promises = spaceList.value.map(async (s) => {
      try {
        const pr = await getSpacePermission(s.systemId)
        permMap.value[s.systemId] = pr.data || { canEdit: false, isAdmin: false }
      } catch { permMap.value[s.systemId] = { canEdit: false, isAdmin: false } }
    })
    await Promise.all(promises)
  } catch { /* handled */ }
  loading.value = false
}

const enterSpace = (systemId: string) => {
  router.push(`/space/${systemId}/intro`)
}

const openCreate = () => {
  form.value = { systemName: '', systemCode: '', owner: '', description: '' }
  showCreate.value = true
}

const handleCreate = async () => {
  if (!form.value.systemName || !form.value.systemCode) {
    ElMessage.warning('请填写系统名称和代号'); return
  }
  creating.value = true
  try {
    await createSpace(form.value)
    ElMessage.success('创建成功')
    showCreate.value = false
    await loadSpaces()
  } catch { /* handled */ }
  creating.value = false
}

const openEdit = (space: Space) => {
  editForm.value = {
    systemId: space.systemId,
    systemName: space.systemName,
    systemCode: space.systemCode,
    description: space.description || ''
  }
  showEdit.value = true
}

const handleEdit = async () => {
  if (!editForm.value.systemName) {
    ElMessage.warning('系统名称不能为空'); return
  }
  editing.value = true
  try {
    await updateSpace(editForm.value.systemId, {
      systemName: editForm.value.systemName,
      systemCode: editForm.value.systemCode,
      description: editForm.value.description
    })
    ElMessage.success('保存成功')
    showEdit.value = false
    await loadSpaces()
  } catch { /* handled */ }
  editing.value = false
}

const handleDelete = async (space: Space) => {
  try {
    await ElMessageBox.confirm(
      `确定删除系统「${space.systemName}」？此操作将删除该系统下的所有内容（百科、拓扑、SQL库等），且不可恢复。`,
      '确认删除',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteSpace(space.systemId)
    ElMessage.success('已删除')
    await loadSpaces()
  } catch { /* cancelled */ }
}

onMounted(loadSpaces)
</script>
