<template>
  <div style="display:flex;flex-direction:column;height:calc(100vh - 140px)">
    <div class="page-header">
      <h3>AI智能问答</h3>
      <el-switch v-model="useThink" active-text="深度推理" inactive-text="快速问答" />
    </div>

    <!-- 对话区 -->
    <div ref="messagesRef" style="flex:1;overflow-y:auto;padding:16px;background:#fafafa;border-radius:8px;margin-bottom:16px">
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
import { ref, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useSSE } from '@/composables/useSSE'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

interface Msg { id: number; role: 'user' | 'assistant'; content: string }

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const messages = ref<Msg[]>([])
const input = ref('')
const loading = ref(false)
const useThink = ref(false)
const messagesRef = ref<HTMLElement>()
const { sendMessage } = useSSE()

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

const handleSend = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''

  messages.value.push({ id: Date.now(), role: 'user', content: text })
  const aiMsg: Msg = { id: Date.now() + 1, role: 'assistant', content: '' }
  messages.value.push(aiMsg)
  loading.value = true
  scrollToBottom()

  await sendMessage(
    `/api/spaces/${systemId.value}/ai/chat`,
    { message: text, model: useThink.value ? 'think' : 'flash' },
    (chunk) => { aiMsg.content += chunk; scrollToBottom() },
    () => { loading.value = false },
    (err) => { aiMsg.content = '错误: ' + err; loading.value = false }
  )
}
</script>

<style scoped>
.typing-dots { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.3; } }
</style>
