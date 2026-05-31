<template>
  <div style="max-width:1100px;margin:0 auto;padding:20px">
    <div class="page-header">
      <h3>用户管理</h3>
    </div>
    <el-table :data="userList" v-loading="loading" border stripe>
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column label="角色" width="130">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'EDITOR' ? 'primary' : 'info'" size="small">
            {{ roleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可管理系统" min-width="220">
        <template #default="{ row }">
          <template v-if="row.role === 'ADMIN'">所有系统</template>
          <template v-else-if="row.systems && row.systems.length">
            <el-tag v-for="s in row.systems" :key="s.systemId" size="small" style="margin-right:4px;margin-bottom:4px">
              {{ s.systemName }}
            </el-tag>
          </template>
          <span v-else style="color:#c0c4cc">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="{ row }">
          <div v-if="row.username !== 'admin'" style="display:flex;align-items:center;flex-wrap:wrap;gap:6px">
            <el-select v-model="row.role" size="small" style="width:100px"
                       @change="handleRoleChange(row)">
              <el-option label="ADMIN" value="ADMIN" />
              <el-option label="EDITOR" value="EDITOR" />
              <el-option label="VIEWER" value="VIEWER" />
            </el-select>
            <el-button size="small" @click="openResetPwd(row)">重置密码</el-button>
            <el-button v-if="row.role === 'EDITOR'" type="primary" size="small" @click="openAuth(row)">系统授权</el-button>
          </div>
          <span v-else style="color:#909399;font-size:12px">超级管理员</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 系统授权对话框 -->
    <el-dialog v-model="showAuth" :title="`系统授权 - ${authUser?.nickname || authUser?.username}`" width="550px">
      <p style="color:#909399;margin-bottom:16px">勾选该用户可以编辑的系统：</p>
      <div v-loading="authLoading">
        <div v-for="space in allSpaces" :key="space.systemId" style="display:flex;align-items:center;padding:10px 0;border-bottom:1px solid #f0f0f0">
          <el-checkbox :model-value="isMember(space.systemId)" @change="toggleMember(space, $event)" style="flex:1">
            <div>
              <strong>{{ space.systemName }}</strong>
              <span style="color:#909399;margin-left:8px">{{ space.systemCode }}</span>
            </div>
          </el-checkbox>
        </div>
        <el-empty v-if="allSpaces.length === 0" description="暂无系统空间" />
      </div>
      <template #footer>
        <el-button @click="showAuth = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="showResetPwd" :title="`重置密码 - ${resetPwdUser?.nickname || resetPwdUser?.username}`" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="不少于6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetPwd = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserRole, resetUserPassword, getSystemMembers, addSystemMember, removeSystemMember } from '@/api/auth'
import { getSpaceList } from '@/api/space'

const userList = ref<any[]>([])
const loading = ref(false)
const showAuth = ref(false)
const authLoading = ref(false)
const authUser = ref<any>(null)
const allSpaces = ref<any[]>([])
const memberUserIds = ref<Set<string>>(new Set())

// 重置密码
const showResetPwd = ref(false)
const resetPwdUser = ref<any>(null)
const newPassword = ref('')
const resetPwdLoading = ref(false)

const openResetPwd = (user: any) => {
  resetPwdUser.value = user
  newPassword.value = ''
  showResetPwd.value = true
}

const handleResetPwd = async () => {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码长度不能少于6位'); return
  }
  resetPwdLoading.value = true
  try {
    await resetUserPassword(resetPwdUser.value.userId, newPassword.value)
    ElMessage.success('密码已重置')
    showResetPwd.value = false
  } catch { /* handled */ }
  resetPwdLoading.value = false
}

const roleLabel = (role: string) => {
  const map: Record<string, string> = { ADMIN: '超级管理员', EDITOR: '系统管理员', VIEWER: '访客' }
  return map[role] || role
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList()
    const users = res.data || []
    // 为每个用户加载其可管理的系统
    const spaceRes = await getSpaceList()
    const spaces = spaceRes.data || []
    for (const user of users) {
      if (user.role === 'ADMIN') {
        user.systems = spaces.map((s: any) => ({ systemId: s.systemId, systemName: s.systemName }))
        continue
      }
      if (user.role === 'VIEWER') {
        user.systems = []
        continue
      }
      // EDITOR: 遍历每个系统检查成员关系
      user.systems = []
      for (const space of spaces) {
        try {
          const membersRes = await getSystemMembers(space.systemId)
          const members = membersRes.data || []
          if (members.some((m: any) => m.userId === user.userId)) {
            user.systems.push({ systemId: space.systemId, systemName: space.systemName })
          }
        } catch { /* skip */ }
      }
    }
    userList.value = users
  } catch { /* handled */ }
  loading.value = false
}

const handleRoleChange = async (user: any) => {
  try {
    await updateUserRole(user.userId, user.role)
    ElMessage.success('角色已更新')
    await loadData()
  } catch { await loadData() }
}

// 系统授权
const openAuth = async (user: any) => {
  authUser.value = user
  showAuth.value = true
  authLoading.value = true
  try {
    const spaceRes = await getSpaceList()
    allSpaces.value = spaceRes.data || []
    // 获取每个系统的成员，找出当前用户在哪些系统中
    memberUserIds.value = new Set()
    for (const space of allSpaces.value) {
      try {
        const res = await getSystemMembers(space.systemId)
        const members = res.data || []
        if (members.some((m: any) => m.userId === user.userId)) {
          memberUserIds.value.add(space.systemId)
        }
      } catch { /* skip */ }
    }
  } catch { /* handled */ }
  authLoading.value = false
}

const isMember = (systemId: string) => memberUserIds.value.has(systemId)

const toggleMember = async (space: any, checked: boolean) => {
  try {
    if (checked) {
      await addSystemMember(space.systemId, authUser.value.userId, 'ADMIN')
      memberUserIds.value.add(space.systemId)
      ElMessage.success(`已授权「${space.systemName}」`)
    } else {
      await removeSystemMember(space.systemId, authUser.value.userId)
      memberUserIds.value.delete(space.systemId)
      ElMessage.success(`已移除「${space.systemName}」`)
    }
    await loadData()
  } catch { /* handled */ }
}

onMounted(loadData)
</script>
