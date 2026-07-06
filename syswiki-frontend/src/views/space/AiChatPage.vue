<template>
  <div class="chat-page">
    <div class="page-header">
      <h3>AI智能问答</h3>
      <div class="chat-toolbar">
        <el-switch v-model="useThink" active-text="深度推理" inactive-text="快速问答" />
        <el-button size="small" @click="handleClear">清空对话</el-button>
      </div>
    </div>

    <!-- 对话区 -->
    <div ref="messagesRef" class="chat-messages">

      <!-- 开场白 -->
      <div v-if="messages.length === 0" class="welcome-section">
        <div class="welcome-row">
          <el-avatar :size="40" class="avatar-ai">AI</el-avatar>
          <div class="welcome-content">
            <div class="ai-bubble">
              <p class="m-0 mb-sm">你好！我是系统百科 AI 助手，可以回答关于 <strong>{{ spaceStore.currentSystemName }}</strong> 的各类问题。</p>
              <p class="m-0 text-muted">你可以直接输入问题，或点击下方推荐问题开始：</p>
            </div>
            <div class="suggest-wrap">
              <el-button v-for="q in welcomeQuestions" :key="q" class="suggest-btn" @click="sendQuestion(q)">{{ q }}</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <div class="msg-row"
             :class="{ 'msg-row--user': msg.role === 'user' }">
          <el-avatar :size="36"
                     :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
            {{ msg.role === 'user' ? 'U' : 'AI' }}
          </el-avatar>
          <div class="msg-bubble"
               :class="msg.role === 'user' ? 'msg-bubble--user' : 'msg-bubble--ai'">
            <MarkdownViewer v-if="msg.role === 'assistant' && msg.content" :content="cleanContent(msg.content)" />
            <span v-else>{{ msg.content }}</span>
          </div>
        </div>
        <!-- AI 回答末尾的推荐问题 -->
        <div v-if="msg.role === 'assistant' && !loading && idx === messages.length - 1 && getFollowUps(msg.content).length"
             class="follow-up-row">
          <div class="follow-up-spacer"></div>
          <div class="ml-md">
            <div class="suggest-wrap">
              <el-button v-for="q in getFollowUps(msg.content)" :key="q" class="suggest-btn" @click="sendQuestion(q)">{{ q }}</el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-row">
        <el-avatar :size="36" class="avatar-ai">AI</el-avatar>
        <div class="loading-bubble">
          <span class="typing-dots">思考中...</span>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input-area">
      <el-input v-model="input" type="textarea" :rows="2" placeholder="输入问题，Ctrl+Enter发送"
                @keydown.ctrl.enter="handleSend" />
      <el-button type="primary" :disabled="!input.trim() || loading" @click="handleSend" class="h-auto">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSSE } from '@/composables/useSSE'
import { useAiStore } from '@/stores/ai'
import { useSpaceStore } from '@/stores/space'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'
import { sanitizeHtml } from '@/utils/sanitize'
import { ErrorType } from '@/utils/errorHandler'

defineOptions({ name: 'AiChatPage' })

const route = useRoute()
const systemId = computed(() => route.params.systemId as string)
const aiStore = useAiStore()
const spaceStore = useSpaceStore()
const { sendMessage } = useSSE()

const input = ref('')
const loading = ref(false)
const useThink = ref(false)
const messagesRef = ref<HTMLElement>()

const messages = computed(() => aiStore.getMessages(systemId.value))

const welcomeQuestions = [
  '这个系统的核心功能是什么？',
  '系统的服务器配置是怎样的？',
  '查询交易流水用什么SQL？',
  '系统的上下游链路是怎样的？'
]

const getFollowUps = (content: string): string[] => {
  if (!content) return []
  const match = content.match(/【推荐问题】\s*\n([\s\S]*?)$/)
  if (!match) return []
  return match[1].split('\n').map(l => l.trim()).filter(l => l.length > 2).slice(0, 3)
}

const cleanContent = (content: string): string => {
  if (!content) return ''
  const cleaned = content.replace(/\s*【推荐问题】[\s\S]*$/, '').trim()
  return sanitizeHtml(cleaned)
}

const scrollToBottom = () => {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}

const sendQuestion = (q: string) => { input.value = q; handleSend() }

const handleSend = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  aiStore.addUserMessage(systemId.value, text)
  aiStore.addAssistantMessage(systemId.value)
  loading.value = true
  scrollToBottom()

  await sendMessage(
    `/api/spaces/${systemId.value}/ai/chat`,
    { message: text, model: useThink.value ? 'think' : 'flash' },
    (chunk) => { aiStore.appendToLastMessage(systemId.value, chunk); scrollToBottom() },
    () => { loading.value = false },
    (err) => {
      // 认证错误已在 useSSE 中分类，组件展示友好提示即可
      const prefix = err.type === ErrorType.AUTH_ERROR ? '[认证] ' : ''
      aiStore.appendToLastMessage(systemId.value, '错误: ' + prefix + err.message)
      loading.value = false
    }
  )
}

const handleClear = () => { aiStore.clearMessages(systemId.value) }

watch(messages, () => scrollToBottom(), { deep: true })
</script>

<style scoped>
.ai-bubble { background: #fff; padding: 12px 16px; border-radius: 8px; line-height: 1.8; font-size: 14px; }
.suggest-wrap { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.suggest-btn { font-size: 13px; border-radius: 16px; border-color: #d9ecff; color: #409eff; background: #ecf5ff; }
.suggest-btn:hover { background: #409eff; color: #fff; border-color: #409eff; }
.typing-dots { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.3; } }
</style>
