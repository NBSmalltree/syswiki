import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
}

export const useAiStore = defineStore('ai', () => {
  // 按 systemId 隔离对话历史
  const chatHistory = ref<Map<string, ChatMessage[]>>(new Map())

  function getMessages(systemId: string): ChatMessage[] {
    return chatHistory.value.get(systemId) || []
  }

  function addUserMessage(systemId: string, content: string) {
    const list = chatHistory.value.get(systemId) || []
    list.push({ id: Date.now(), role: 'user', content })
    chatHistory.value.set(systemId, list)
  }

  function addAssistantMessage(systemId: string): ChatMessage {
    const list = chatHistory.value.get(systemId) || []
    const msg: ChatMessage = { id: Date.now() + 1, role: 'assistant', content: '' }
    list.push(msg)
    chatHistory.value.set(systemId, list)
    return msg
  }

  function appendToLastMessage(systemId: string, chunk: string) {
    const list = chatHistory.value.get(systemId) || []
    const last = list[list.length - 1]
    if (last && last.role === 'assistant') {
      last.content += chunk
    }
  }

  function clearMessages(systemId: string) {
    chatHistory.value.delete(systemId)
  }

  return { chatHistory, getMessages, addUserMessage, addAssistantMessage, appendToLastMessage, clearMessages }
})
