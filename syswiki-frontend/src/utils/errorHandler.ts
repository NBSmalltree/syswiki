import { ElMessage } from 'element-plus'
import router from '@/router'

/** 错误类型枚举 */
export enum ErrorType {
  /** 业务逻辑错误（后端返回非200状态码或业务code） */
  BUSINESS_ERROR = 'BUSINESS_ERROR',
  /** 网络连接错误（无法到达服务器、超时等） */
  NETWORK_ERROR = 'NETWORK_ERROR',
  /** 认证错误（401/403） */
  AUTH_ERROR = 'AUTH_ERROR',
  /** 未知错误 */
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

/** 统一的业务错误类 */
export class AppError extends Error {
  type: ErrorType
  /** 后端返回的业务code，仅 BUSINESS_ERROR 有值 */
  code?: number
  /** 原始错误对象 */
  originalError?: unknown

  constructor(type: ErrorType, message: string, options?: { code?: number; originalError?: unknown }) {
    super(message)
    this.name = 'AppError'
    this.type = type
    this.code = options?.code
    this.originalError = options?.originalError
  }
}

/**
 * 统一错误处理器
 *
 * 集中处理所有类型的错误，组件只需关心业务逻辑，不需要重复编写 catch 逻辑。
 */
export class ErrorHandler {
  /**
   * 处理 axios 响应拦截器中的业务错误（后端返回 code !== 200）
   * @param message  后端返回的错误信息
   * @param code     后端返回的业务码
   */
  static handleBusinessError(message: string, code?: number): never {
    const appError = new AppError(ErrorType.BUSINESS_ERROR, message || '请求失败', { code })
    ElMessage.error(appError.message)
    throw appError
  }

  /**
   * 处理认证错误（401/403）
   * 清除本地认证信息并跳转到登录页
   */
  static handleAuthError(status: number, message?: string): never {
    // 清除认证信息
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('role')

    const msg = status === 401 ? '登录已过期，请重新登录' : '没有访问权限'
    ElMessage.error(msg)

    // 跳转登录页
    router.push('/login')

    throw new AppError(ErrorType.AUTH_ERROR, message || msg)
  }

  /**
   * 处理网络错误（无法连接、超时等）
   */
  static handleNetworkError(originalError: unknown): never {
    const err = originalError as { code?: string; message?: string }
    let msg = '网络错误，请检查网络连接'

    if (err?.code === 'ECONNABORTED') {
      msg = '请求超时，请稍后重试'
    } else if (err?.message?.includes('Network Error')) {
      msg = '网络连接失败，请检查网络'
    }

    ElMessage.error(msg)
    throw new AppError(ErrorType.NETWORK_ERROR, msg, { originalError })
  }

  /**
   * 处理未知错误（兜底）
   */
  static handleUnknownError(originalError: unknown): never {
    console.error('[ErrorHandler] Unknown error:', originalError)
    ElMessage.error('发生未知错误，请稍后重试')
    throw new AppError(ErrorType.UNKNOWN_ERROR, '发生未知错误', { originalError })
  }

  /**
   * 统一处理所有 HTTP 响应错误（供 axios 拦截器使用）
   */
  static handleHttpError(error: { response?: { status: number; data?: { message?: string } }; code?: string; message?: string }): never {
    const status = error.response?.status
    const serverMessage = error.response?.data?.message

    // 认证错误
    if (status === 401 || status === 403) {
      return ErrorHandler.handleAuthError(status, serverMessage)
    }

    // 服务器错误 / 其它 HTTP 错误
    if (status) {
      return ErrorHandler.handleNetworkError(error)
    }

    // 无 response —— 网络层错误
    return ErrorHandler.handleNetworkError(error)
  }

  /**
   * 通用 catch 包装器 —— 组件使用此方法替代自己写 catch
   *
   * 如果错误已经被 ErrorHandler 处理过（即已经是 AppError），则直接重新抛出；
   * 否则按照错误类型分发处理。
   *
   * 典型用法：
   * ```ts
   * try {
   *   await someApiCall()
   * } catch (e) {
   *   ErrorHandler.catch(e)
   * }
   * ```
   */
  static catch(error: unknown): never {
    // 已经处理过的 AppError，直接重新抛出
    if (error instanceof AppError) {
      throw error
    }

    // axios 风格的 error
    const httpError = error as { response?: { status: number; data?: { message?: string } }; code?: string; message?: string }
    if (httpError.response || httpError.code) {
      return ErrorHandler.handleHttpError(httpError as any)
    }

    // 其它未知错误
    return ErrorHandler.handleUnknownError(error)
  }
}
