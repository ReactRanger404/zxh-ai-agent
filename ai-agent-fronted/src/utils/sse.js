/**
 * 解析 SSE 流式响应，逐块回调文本内容
 * @param {Response} response fetch 响应对象
 * @param {(chunk: string) => void} onChunk 收到文本块时的回调
 */
export async function parseSseStream(response, onChunk) {
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取响应流')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith(':')) continue

      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim()
        if (data && data !== '[DONE]') {
          onChunk(data)
        }
      } else {
        onChunk(trimmed)
      }
    }
  }

  if (buffer.trim()) {
    const trimmed = buffer.trim()
    if (trimmed.startsWith('data:')) {
      const data = trimmed.slice(5).trim()
      if (data && data !== '[DONE]') onChunk(data)
    } else {
      onChunk(trimmed)
    }
  }
}
