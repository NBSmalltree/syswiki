import axios from 'axios'
import { ErrorHandler } from '@/utils/errorHandler'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

/** 是否正在刷新 Token，用于拦截并发请求 */
let isRefreshing = false
/** 等待刷新完成的请求队列 */
let pendingQueue: Array<{ resolve: (token: string) => void; reject: (err: any) => void }> = []

// 请求拦截器：自动携带Token
request.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理 + 401 自动刷新
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) return { code, message, data }
    ErrorHandler.handleBusinessError(message, code)
    return Promise.reject(new Error(message || '业务处理失败'))
  },
  async (error) => {
    const originalRequest = error.config

    // 非 401 或已经是刷新请求，直接拒绝
    if (error.response?.status !== 401 || originalRequest.url?.includes('/auth/refresh')) {
      ErrorHandler.handleHttpError(error)
      return Promise.reject(error)
    }

    if (isRefreshing) {
      // 已有刷新在进行中，排队等待新 token
      return new Promise((resolve, reject) => {
        pendingQueue.push({
          resolve: (newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          },
          reject
        })
      })
    }

    isRefreshing = true
    const authStore = useAuthStore()
    const refreshed = await authStore.tryRefresh()

    if (refreshed) {
      // 刷新成功，重放排队请求
      const newToken = authStore.token
      pendingQueue.forEach(({ resolve }) => resolve(newToken))
      pendingQueue = []
      isRefreshing = false

      // 重放当前请求
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return request(originalRequest)
    }

    // 刷新失败，拒绝所有排队请求
    pendingQueue.forEach(({ reject }) => reject(error))
    pendingQueue = []
    isRefreshing = false

    authStore.logout()
    window.location.href = '/login'
    return Promise.reject(error)
  }
)

export default request
