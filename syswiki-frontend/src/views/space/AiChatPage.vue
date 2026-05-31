<template>
  <div style="display:flex;flex-direction:column;height:calc(100vh - 140px)">
    <div class="page-header">
      <h3>AI智能问答</h3>
      <div style="display:flex;align-items:center;gap:12px">
        <el-switch v-model="useThink" active-text="深度推理" inactive-text="快速问答" />
        <el-button size="small" @click="handleClear">清空对话</el-button>
      </div>
    </div>

    <!-- 对话区 -->
    <div ref="messagesRef" style="flex:1;overflow-y:auto;padding:16px;background:#fafafa;border-radius:8px;margin-bottom:16px">
      <div v-if="messages.length === 0" style="text-align:center;color:#c0c4cc;padding:60px 0">
        <p>输入问题开始对话</p>
      </div>
      <div v-for="msg in messages" :key="msg.id" style="display:flex;margin-bottom:16px"
           :style="{ flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }">
        <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a', flexShrink: 0 }">
          {{ msg.role === 'user' ? 'U' : 'AI' }}
        </el-avatar>
        <div style="max-width:70%;padding:10px 14px;border-radius:8px;margin:0 10px"
             :style="{ background: msg.role === 'user' ? '#409eff' : '#fff', color: msg.role === 'user' ? '#fff' : '#333' }">
          <MarkdownViewer v-if="msg.role === 'assistant' && msg.content" :content="msg.content" />
          <span v-else>{{ msg.content }}</span>
        </div>
      </div>
      <div v-if="loading" style="display:flex;margin-bottom:16px">
        <el-avatar :size="36" style="background:#67c23a;flex-shrink:0">AI</el-avatar>
        <div style="padding:10px 14px;background:#fff;border-radius:8px;margin-left:10px">
          <span class="typing-dots">思考中...</span>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div style="display:flex;gap:8px">
      <el-input v-model="input" type="textarea" :rows="2" placeholder="输入问题，Ctrl+Enter发送"
                @keydown.ctrl.enter="handleSend" />
      <el-button type="primary" :disabled="!input.trim() || loading" @click="handleSend" style="height:auto">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'

defineOptions({ name: 'AiChatPage' })

import { useRoute } from 'vue-router'
import { useSSE } from '@/composables/useSSE'
import { useAiStore } from '@/stores/ai'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const aiStore = useAiStore()
const { sendMessage } = useSSE()

const input = ref('')
const loading = ref(false)
const useThink = ref(false)
const messagesRef = ref<HTMLElement>()

const messages = computed(() => aiStore.getMessages(systemId.value))

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

const handleSend = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''

  aiStore.addUserMessage(systemId.value, text)
  const aiMsg = aiStore.addAssistantMessage(systemId.value)
  loading.value = true
  scrollToBottom()

  await sendMessage(
    `/api/spaces/${systemId.value}/ai/chat`,
    { message: text, model: useThink.value ? 'think' : 'flash' },
    (chunk) => {
      aiStore.appendToLastMessage(systemId.value, chunk)
      scrollToBottom()
    },
    () => { loading.value = false },
    (err) => {
      aiStore.appendToLastMessage(systemId.value, '错误: ' + err)
      loading.value = false
    }
  )
}

const handleClear = () => {
  aiStore.clearMessages(systemId.value)
}

// 进入页面时滚到底部
watch(messages, () => scrollToBottom(), { deep: true })
</script>

<style scoped>
.typing-dots { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.3; } }
</style>
