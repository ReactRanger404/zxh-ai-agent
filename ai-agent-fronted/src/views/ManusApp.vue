<template>
  <ChatPage
    title="AI 超级智能体"
    placeholder="描述你的问题或任务..."
    :chat-fn="doChatWithManus"
  />
</template>

<script setup>
import ChatPage from './ChatPage.vue'
import { doChatWithManus } from '../api/chat.js'
</script>
