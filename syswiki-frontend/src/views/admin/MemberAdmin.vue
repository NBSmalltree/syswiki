<template>
  <div style="max-width:900px;margin:0 auto;padding:20px">
    <div class="page-header">
      <h3>成员管理</h3>
    </div>

    <!-- 系统选择 -->
    <el-card style="margin-bottom:20px">
      <el-form label-width="80px">
        <el-form-item label="选择系统">
          <el-select v-model="selectedSystemId" placeholder="请选择要管理的系统" style="width:100%"
                     @change="onSystemChange" :loading="systemsLoading">
            <el-option v-for="s in mySystems" :key="s.systemId"
                       :label="`${s.systemName} (${s.systemCode})`" :value="s.systemId" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成员列表 -->
    <template v-if="selectedSystemId">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <h4 style="margin:0">「{{ selectedSystemName }}」的成员</h4>
        <el-button type="primary" size="small" @click="showAdd = true">添加成员</el-button>
      </div>
      <el-table :data="members" v-loading="loading" border stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column label="系统角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'OWNER' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'OWNER' ? '所有者' : '管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'OWNER'" type="danger" size="small" link @click="handleRemove(row)">移除</el-button>
            <span v-else style="color:#909399;font-size:12px">不可移除</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && members.length === 0" description="暂无成员" />
    </template>
    <el-empty v-else description="请先选择一个系统" />

    <!-- 添加成员对话框 -->
    <el-dialog v-model="showAdd" title="添加系统成员" width="450px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-select v-model="selectedUserId" filterable placeholder="搜索用户名" style="width:100%">
            <el-option v-for="u in availableUsers" :key="u.userId"
                       :label="`${u.nickname} (${u.username})`" :value="u.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSpaceList } from '@/api/space'
import { getUserList, getSystemMembers, addSystemMember, removeSystemMember } from '@/api/auth'

const mySystems = ref<any[]>([])
const systemsLoading = ref(false)
const selectedSystemId = ref('')
const members = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const adding = ref(false)
const selectedUserId = ref('')
const allUsers = ref<any[]>([])

const selectedSystemName = computed(() =>
  mySystems.value.find(s => s.systemId === selectedSystemId.value)?.systemName || ''
)

const memberIds = computed(() => new Set(members.value.map(m => m.userId)))
const availableUsers = computed(() =>
  allUsers.value.filter(u => u.role === 'EDITOR' && !memberIds.value.has(u.userId))
)

const loadSystems = async () => {
  systemsLoading.value = true
  try {
    const res = await getSpaceList('mine')
    mySystems.value = res.data || []
  } catch { /* handled */ }
  systemsLoading.value = false
}

const loadMembers = async () => {
  if (!selectedSystemId.value) return
  loading.value = true
  try {
    const res = await getSystemMembers(selectedSystemId.value)
    members.value = res.data || []
  } catch { /* handled */ }
  loading.value = false
}

const loadUsers = async () => {
  try {
    const res = await getUserList()
    allUsers.value = (res.data || []).filter((u: any) => u.role !== 'ADMIN')
  } catch { /* handled */ }
}

const onSystemChange = () => {
  members.value = []
  selectedUserId.value = ''
  loadMembers()
}

const handleAdd = async () => {
  if (!selectedUserId.value) { ElMessage.warning('请选择用户'); return }
  adding.value = true
  try {
    await addSystemMember(selectedSystemId.value, selectedUserId.value, 'ADMIN')
    ElMessage.success('已添加')
    showAdd.value = false
    selectedUserId.value = ''
    await loadMembers()
  } catch { /* handled */ }
  adding.value = false
}

const handleRemove = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确定移除「${user.nickname || user.username}」？`, '确认', { type: 'warning' })
    await removeSystemMember(selectedSystemId.value, user.userId)
    ElMessage.success('已移除')
    await loadMembers()
  } catch { /* cancelled */ }
}

onMounted(() => { loadSystems(); loadUsers() })
</script>
