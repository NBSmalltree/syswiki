import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isEditor = computed(() => role.value === 'EDITOR' || role.value === 'ADMIN')
  const displayName = computed(() => nickname.value || username.value)

  function setAuth(data: { token: string; username: string; nickname: string; role: string }) {
    token.value = data.token
    username.value = data.username
    nickname.value = data.nickname
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    username.value = ''
    nickname.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    localStorage.removeItem('role')
  }

  return { token, username, nickname, role, isLoggedIn, isAdmin, isEditor, displayName, setAuth, logout }
})
