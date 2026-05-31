<template>
  <div class="chat-page">
    <header class="chat-header">
      <router-link to="/" class="back-btn">← 返回</router-link>
      <div class="header-info">
        <h1>{{ title }}</h1>
        <p v-if="chatId" class="chat-id">会话 ID: {{ chatId }}</p>
      </div>
    </header>

    <ChatRoom
      :messages="messages"
      :loading="loading"
      :placeholder="placeholder"
      @send="handleSend"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { generateChatId } from '../utils/id.js'
import { parseSseStream } from '../utils/sse.js'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  placeholder: {
    type: String,
    default: '输入消息，按 Enter 发送...'
  },
  /** 是否自动生成 chatId */
  withChatId: {
    type: Boolean,
    default: false
  },
  /** SSE 请求函数: (message, chatId?, signal?) => Promise<Response> */
  chatFn: {
    type: Function,
    required: true
  }
})

const chatId = ref('')
const messages = ref([])
const loading = ref(false)
let abortController = null

onMounted(() => {
  if (props.withChatId) {
    chatId.value = generateChatId()
  }
})

async function handleSend(text) {
  if (!text.trim() || loading.value) return

  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })

  const aiIndex = messages.value.length - 1
  loading.value = true

  if (abortController) {
    abortController.abort()
  }
  abortController = new AbortController()

  try {
    const response = props.withChatId
      ? await props.chatFn(text, chatId.value, abortController.signal)
      : await props.chatFn(text, abortController.signal)

    await parseSseStream(response, (chunk) => {
      messages.value[aiIndex].content += chunk
    })
  } catch (err) {
    if (err.name === 'AbortError') return
    messages.value[aiIndex].content = `请求出错: ${err.message || '未知错误'}`
  } finally {
    loading.value = false
    abortController = null
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.back-btn {
  font-size: 0.9rem;
  color: #6366f1;
  white-space: nowrap;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.15s;
}

.back-btn:hover {
  background: #eef2ff;
}

.header-info h1 {
  font-size: 1.15rem;
  font-weight: 600;
  color: #1f2937;
}

.chat-id {
  font-size: 0.75rem;
  color: #9ca3af;
  margin-top: 2px;
  word-break: break-all;
}
</style>
