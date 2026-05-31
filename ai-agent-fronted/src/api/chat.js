import request from './request'

const BASE_URL = '/api'

/**
 * 通过 SSE 调用 AI 恋爱大师接口
 * @param {string} message 用户消息
 * @param {string} chatId 会话 ID
 * @param {AbortSignal} signal 取消信号
 */
export function doChatWithLoveAppSse(message, chatId, signal) {
  const params = new URLSearchParams({ message, chatId })
  return fetch(`${BASE_URL}/ai/love_app/chat/sse?${params}`, {
    method: 'GET',
    headers: { Accept: 'text/event-stream' },
    signal
  })
}

/**
 * 通过 SSE 调用 AI 超级智能体接口
 * @param {string} message 用户消息
 * @param {AbortSignal} signal 取消信号
 */
export function doChatWithManus(message, signal) {
  const params = new URLSearchParams({ message })
  return fetch(`${BASE_URL}/ai/manus/chat?${params}`, {
    method: 'GET',
    headers: { Accept: 'text/event-stream' },
    signal
  })
}

export { request }
