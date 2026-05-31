<template>
  <ChatPage
    title="AI 恋爱大师"
    placeholder="说说你的情感困惑..."
    :with-chat-id="true"
    :chat-fn="doChatWithLoveAppSse"
  />
</template>

<script setup>
import ChatPage from './ChatPage.vue'
import { doChatWithLoveAppSse } from '../api/chat.js'
</script>
