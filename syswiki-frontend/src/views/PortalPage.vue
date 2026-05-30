<template>
  <div class="portal-page" style="max-width:1200px;margin:0 auto;padding:40px 20px">
    <div style="text-align:center;margin-bottom:40px">
      <h1 style="font-size:32px;color:#303133;margin-bottom:8px">系统百科平台</h1>
      <p style="color:#909399;font-size:16px">统一系统知识管理，打破信息孤岛</p>
    </div>

    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h3>已入驻系统</h3>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon> 创建系统空间
      </el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-for="space in spaceList" :key="space.systemId" style="margin-bottom:20px">
        <el-card shadow="hover" style="cursor:pointer" @click="enterSpace(space.systemId)">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <h4 style="margin:0">{{ space.systemName }}</h4>
            <el-tag size="small">{{ space.systemCode }}</el-tag>
          </div>
          <p style="color:#909399;font-size:13px;margin:4px 0">负责人：{{ space.owner }}</p>
          <p style="color:#909399;font-size:13px;margin:4px 0">更新：{{ formatDate(space.updateTime) }}</p>
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
        <el-form-item label="负责人" required>
          <el-input v-model="form.owner" placeholder="系统负责人" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSpaceList, createSpace } from '@/api/space'
import { formatDate } from '@/utils/date'
import type { Space, CreateSpaceForm } from '@/types/space'

const router = useRouter()
const spaceList = ref<Space[]>([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const form = ref<CreateSpaceForm>({ systemName: '', systemCode: '', owner: '', description: '' })

const loadSpaces = async () => {
  loading.value = true
  try {
    const res = await getSpaceList()
    spaceList.value = res.data || []
  } catch { /* handled */ }
  loading.value = false
}

const enterSpace = (systemId: string) => {
  router.push(`/space/${systemId}/intro`)
}

const handleCreate = async () => {
  if (!form.value.systemName || !form.value.systemCode || !form.value.owner) {
    ElMessage.warning('请填写必填项')
    return
  }
  creating.value = true
  try {
    await createSpace(form.value)
    ElMessage.success('创建成功')
    showCreate.value = false
    form.value = { systemName: '', systemCode: '', owner: '', description: '' }
    await loadSpaces()
  } catch { /* handled */ }
  creating.value = false
}

onMounted(loadSpaces)
</script>
