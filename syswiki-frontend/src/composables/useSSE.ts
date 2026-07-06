import { ErrorHandler, ErrorType } from '@/utils/errorHandler'

/** SSE 错误信息，包含类型以便调用方按需处理 */
export interface SSEError {
  type: ErrorType
  message: string
}

export function useSSE() {
  const sendMessage = async (
    url: string,
    body: { message: string; model: string },
    onChunk: (chunk: string) => void,
    onDone: () => void,
    onError: (err: SSEError) => void
  ) => {
    try {
      const token = sessionStorage.getItem('token')
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(body)
      })
      if (!response.ok) {
        const type = response.status === 401 || response.status === 403
          ? ErrorType.AUTH_ERROR
          : ErrorType.NETWORK_ERROR
        const message = response.status === 401 ? '登录已过期，请重新登录'
          : response.status === 403 ? '没有访问权限'
          : `请求失败: ${response.status}`
        onError({ type, message })
        return
      }
      const reader = response.body?.getReader()
      const decoder = new TextDecoder()
      if (!reader) { onError({ type: ErrorType.NETWORK_ERROR, message: '无法读取响应流' }); return }

      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          const trimmed = line.trim()
          if (trimmed.startsWith('data:')) {
            const jsonStr = trimmed.slice(5).trim()
            if (!jsonStr) continue
            try {
              const data = JSON.parse(jsonStr)
              if (data.content && data.content !== 'null') onChunk(data.content)
              if (data.done) { onDone(); return }
            } catch { /* skip malformed SSE data */ }
          }
        }
      }
      onDone()
    } catch (err: any) {
      // 网络层错误
      onError({ type: ErrorType.NETWORK_ERROR, message: err.message || '网络错误，请检查网络连接' })
    }
  }
  return { sendMessage }
}
