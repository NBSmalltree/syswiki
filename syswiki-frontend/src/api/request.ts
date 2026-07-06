import axios from 'axios'
import { ErrorHandler } from '@/utils/errorHandler'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

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

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) return { code, message, data }
    // 业务错误：后端返回了HTTP 200，但业务码不是200
    ErrorHandler.handleBusinessError(message, code)
    return Promise.reject(new Error(message || '业务处理失败'))
  },
  (error) => {
    // 网络错误、HTTP错误（401/403/500等）统一由 ErrorHandler 处理
    ErrorHandler.handleHttpError(error)
    return Promise.reject(error)
  }
)

export default request
