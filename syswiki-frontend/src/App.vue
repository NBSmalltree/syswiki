<template>
  <div v-if="isGuestRoute">
    <router-view />
  </div>
  <el-container v-else class="app-layout">
    <el-header height="60px" class="app-header">
      <div class="header-left" @click="router.push('/portal')" style="cursor:pointer">
        <span class="logo-text">系统百科</span>
      </div>
      <div class="header-center">
        <el-breadcrumb separator-icon="ArrowRight" class="app-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/portal' }">
            <el-icon style="margin-right:4px"><HomeFilled /></el-icon>门户
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="spaceStore.currentSystemName">
            {{ spaceStore.currentSystemName }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <el-tag v-if="authStore.isAdmin" type="danger" size="small" effect="dark">超级管理员</el-tag>
          <el-tag v-else-if="authStore.isEditor" type="primary" size="small" effect="dark">系统管理员</el-tag>
          <el-tag v-else type="info" size="small" effect="dark">访客</el-tag>
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              {{ authStore.displayName }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="authStore.isAdmin" command="users">
                  <el-icon><Setting /></el-icon>用户管理
                </el-dropdown-item>
                <el-dropdown-item v-if="!authStore.isAdmin && authStore.isEditor" command="members">
                  <el-icon><UserFilled /></el-icon>成员管理
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="showPwdDialog" title="修改密码" width="400px" append-to-body>
    <el-form :model="pwdForm" label-width="80px">
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="不少于6位" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showPwdDialog = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, onErrorCaptured } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSpaceStore } from '@/stores/space'
import { useAuthStore } from '@/stores/auth'
import { changeMyPassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const authStore = useAuthStore()

const isGuestRoute = computed(() => route.meta.guest === true)

// 全局错误捕获，防止白屏
onErrorCaptured((err) => {
  console.error('组件渲染异常:', err)
  ElMessage.error('页面渲染异常，请刷新重试')
  return false  // 阻止错误继续向上传播
})

// 修改密码
const showPwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const handleChangePwd = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请填写旧密码和新密码'); return
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位'); return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致'); return
  }
  pwdLoading.value = true
  try {
    await changeMyPassword(pwdForm.value.oldPassword, pwdForm.value.newPassword)
    ElMessage.success('密码修改成功')
    showPwdDialog.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch { /* handled */ }
  pwdLoading.value = false
}

// 监听路由变化，离开系统空间时清空当前系统
watch(() => route.path, (newPath) => {
  if (!newPath.startsWith('/space/')) {
    spaceStore.clearCurrentSpace()
  }
})

const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (cmd === 'users') {
    router.push('/admin/users')
  } else if (cmd === 'members') {
    router.push('/admin/members')
  } else if (cmd === 'password') {
    showPwdDialog.value = true
  }
}
</script>

<style scoped>
.app-layout { min-height: 100vh; }
.app-header {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #dcdfe6; background: #fff; padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.logo-text {
  font-size: 20px; font-weight: 700; color: #409eff;
  letter-spacing: 2px;
}
.app-breadcrumb {
  font-size: 14px;
}
.header-right {
  display: flex; align-items: center; gap: 14px;
}
.user-dropdown {
  cursor: pointer; display: flex; align-items: center; gap: 4px;
  font-size: 14px; color: #606266;
}
.user-dropdown:hover { color: #409eff; }
.app-main {
  padding: 20px; background: #f5f7fa; min-height: calc(100vh - 60px);
}
</style>
