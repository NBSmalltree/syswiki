<template>
  <div style="display:flex;justify-content:center;align-items:center;height:100vh;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%)">
    <el-card style="width:420px" shadow="always">
      <div style="text-align:center;margin-bottom:30px">
        <h2 style="margin:0;color:#303133">系统百科平台</h2>
        <p style="color:#909399;margin-top:8px">用户登录</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" prefix-icon="Lock" size="large" type="password" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
        <div style="text-align:center">
          <span style="color:#909399">没有账号？</span>
          <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = ref({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form.value)
    authStore.setAuth(res.data)
    ElMessage.success('登录成功')
    router.push('/portal')
  } catch { /* handled by interceptor */ }
  loading.value = false
}
</script>
