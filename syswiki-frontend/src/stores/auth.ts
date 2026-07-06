import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { refreshToken as refreshTokenApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const username = ref(sessionStorage.getItem('username') || '')
  const nickname = ref(sessionStorage.getItem('nickname') || '')
  const role = ref(sessionStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isEditor = computed(() => role.value === 'EDITOR' || role.value === 'ADMIN')
  const displayName = computed(() => nickname.value || username.value)

  function setAuth(data: { token: string; refreshToken?: string; username: string; nickname: string; role: string }) {
    token.value = data.token
    username.value = data.username
    nickname.value = data.nickname
    role.value = data.role
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('username', data.username)
    sessionStorage.setItem('nickname', data.nickname)
    sessionStorage.setItem('role', data.role)
    if (data.refreshToken) {
      refreshToken.value = data.refreshToken
      localStorage.setItem('refreshToken', data.refreshToken)
    }
  }

  /** 静默刷新 access token */
  async function tryRefresh(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const res = await refreshTokenApi(refreshToken.value) as any
      if (res.code === 200 && res.data) {
        setAuth(res.data)
        return true
      }
    } catch { /* refresh failed */ }
    return false
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    username.value = ''
    nickname.value = ''
    role.value = ''
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('role')
    localStorage.removeItem('refreshToken')
  }

  return { token, refreshToken, username, nickname, role, isLoggedIn, isAdmin, isEditor, displayName, setAuth, tryRefresh, logout }
})
