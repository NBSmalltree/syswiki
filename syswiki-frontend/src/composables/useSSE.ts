export function useSSE() {
  const sendMessage = async (
    url: string,
    body: { message: string; model: string },
    onChunk: (chunk: string) => void,
    onDone: () => void,
    onError: (err: string) => void
  ) => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(body)
      })
      if (!response.ok) {
        onError(`请求失败: ${response.status}`)
        return
      }
      const reader = response.body?.getReader()
      const decoder = new TextDecoder()
      if (!reader) { onError('无法读取响应流'); return }

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
            } catch { /* skip malformed */ }
          }
        }
      }
      onDone()
    } catch (err: any) {
      onError(err.message || '网络错误')
    }
  }
  return { sendMessage }
}
