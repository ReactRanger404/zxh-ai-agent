<template>
  <div class="chat-room">
    <div ref="messageListRef" class="message-list">
      <div v-if="messages.length === 0" class="empty-tip">
        <p>{{ emptyText }}</p>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="msg.role"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <span v-if="msg.content">{{ msg.content }}</span>
          <span v-else-if="loading && index === messages.length - 1" class="typing">
            <i></i><i></i><i></i>
          </span>
        </div>
      </div>
    </div>

    <div class="input-area">
      <textarea
        v-model="inputText"
        :placeholder="placeholder"
        :disabled="loading"
        rows="1"
        @keydown.enter.exact.prevent="send"
      />
      <button class="send-btn" :disabled="loading || !inputText.trim()" @click="send">
        {{ loading ? '发送中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: '输入消息...'
  },
  emptyText: {
    type: String,
    default: '开始对话吧，AI 随时为你服务'
  }
})

const emit = defineEmits(['send'])

const inputText = ref('')
const messageListRef = ref(null)

function send() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
}

watch(
  () => props.messages,
  async () => {
    await nextTick()
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  },
  { deep: true }
)
</script>

<style scoped>
.chat-room {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
  font-size: 0.95rem;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 75%;
}

.message-row.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-row.assistant {
  align-self: flex-start;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}

.message-row.user .avatar {
  background: #6366f1;
  color: #fff;
}

.message-row.assistant .avatar {
  background: #e5e7eb;
  color: #374151;
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 0.95rem;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-row.user .bubble {
  background: #6366f1;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .bubble {
  background: #fff;
  color: #1f2937;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 20px;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
  animation: bounce 1.2s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.2s;
}

.typing i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}

.input-area textarea {
  flex: 1;
  resize: none;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 0.95rem;
  line-height: 1.5;
  outline: none;
  max-height: 120px;
  transition: border-color 0.15s;
}

.input-area textarea:focus {
  border-color: #6366f1;
}

.input-area textarea:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.send-btn {
  padding: 10px 20px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  transition: background 0.15s;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.send-btn:disabled {
  background: #c7d2fe;
  cursor: not-allowed;
}
</style>
