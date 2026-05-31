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

      <!-- 开场白 -->
      <div v-if="messages.length === 0" style="padding:20px 0">
        <div style="display:flex;margin-bottom:16px">
          <el-avatar :size="40" style="background:#67c23a;flex-shrink:0">AI</el-avatar>
          <div style="margin-left:12px">
            <div class="ai-bubble">
              <p style="margin:0 0 8px">你好！我是系统百科 AI 助手，可以回答关于 <strong>{{ spaceStore.currentSystemName }}</strong> 的各类问题。</p>
              <p style="margin:0;color:#909399;font-size:13px">你可以直接输入问题，或点击下方推荐问题开始：</p>
            </div>
            <div class="suggest-wrap">
              <el-button v-for="q in welcomeQuestions" :key="q" class="suggest-btn" @click="sendQuestion(q)">{{ q }}</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <div style="display:flex;margin-bottom:16px"
             :style="{ flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }">
          <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a', flexShrink: 0 }">
            {{ msg.role === 'user' ? 'U' : 'AI' }}
          </el-avatar>
          <div style="max-width:70%;padding:10px 14px;border-radius:8px;margin:0 10px"
               :style="{ background: msg.role === 'user' ? '#409eff' : '#fff', color: msg.role === 'user' ? '#fff' : '#333' }">
            <MarkdownViewer v-if="msg.role === 'assistant' && msg.content" :content="cleanContent(msg.content)" />
            <span v-else>{{ msg.content }}</span>
          </div>
        </div>
        <!-- AI 回答末尾的推荐问题 -->
        <div v-if="msg.role === 'assistant' && !loading && idx === messages.length - 1 && getFollowUps(msg.content).length"
             style="display:flex;margin-bottom:16px">
          <div style="width:36px;flex-shrink:0"></div>
          <div style="margin-left:10px">
            <div class="suggest-wrap">
              <el-button v-for="q in getFollowUps(msg.content)" :key="q" class="suggest-btn" @click="sendQuestion(q)">{{ q }}</el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 加载中 -->
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
      <el-button type="primary" :disabled="!input.trim() || loading" @click="handleSend" style="height:auto">发送</el-button>
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
  return content.replace(/\s*【推荐问题】[\s\S]*$/, '').trim()
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
    (err) => { aiStore.appendToLastMessage(systemId.value, '错误: ' + err); loading.value = false }
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
