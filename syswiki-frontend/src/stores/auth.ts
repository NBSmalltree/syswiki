import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const username = ref(sessionStorage.getItem('username') || '')
  const nickname = ref(sessionStorage.getItem('nickname') || '')
  const role = ref(sessionStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isEditor = computed(() => role.value === 'EDITOR' || role.value === 'ADMIN')
  const displayName = computed(() => nickname.value || username.value)

  function setAuth(data: { token: string; username: string; nickname: string; role: string }) {
    token.value = data.token
    username.value = data.username
    nickname.value = data.nickname
    role.value = data.role
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('username', data.username)
    sessionStorage.setItem('nickname', data.nickname)
    sessionStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    username.value = ''
    nickname.value = ''
    role.value = ''
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('role')
  }

  return { token, username, nickname, role, isLoggedIn, isAdmin, isEditor, displayName, setAuth, logout }
})
