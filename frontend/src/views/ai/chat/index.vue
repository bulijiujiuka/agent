<template>
  <div class="chat-container">
    <div class="chat-layout">
      <div class="session-panel">
        <div class="panel-header">
          <span class="panel-title">会话列表</span>
          <el-button type="primary" :icon="Plus" size="small" circle @click="handleNewConversation" />
        </div>
        <div class="session-list">
          <div
            v-for="conv in conversationList"
            :key="conv.id"
            class="session-item"
            :class="{ active: currentConversation?.id === conv.id }"
            @click="selectConversation(conv)"
          >
            <div class="session-icon">💬</div>
            <div class="session-info">
              <div class="session-title">{{ conv.title || '新会话' }}</div>
              <div class="session-time">{{ conv.createTime }}</div>
            </div>
            <el-button
              class="session-delete"
              type="danger"
              :icon="Delete"
              size="small"
              link
              @click.stop="handleDeleteConversation(conv)"
            />
          </div>
          <div v-if="conversationList.length === 0" class="empty-tip">暂无会话，点击 + 新建</div>
        </div>
      </div>

      <div class="chat-panel">
        <div class="chat-header">
          <div class="chat-title">
            <span class="title-dot"></span>
            <span>{{ currentConversation?.title || 'AI 智能问答' }}</span>
          </div>
          <el-tag v-if="currentConversation" size="small" type="success" effect="dark">已连接</el-tag>
        </div>

        <div class="message-area" ref="messageAreaRef">
          <div v-if="!currentConversation" class="welcome-screen">
            <div class="welcome-icon">🤖</div>
            <h3>AI 智能问答助手</h3>
            <p>基于知识库的 RAG 检索增强生成，支持多轮对话</p>
            <el-button type="primary" @click="handleNewConversation">开始新会话</el-button>
          </div>
          <div v-else-if="messageList.length === 0" class="welcome-screen">
            <div class="welcome-icon">💡</div>
            <h3>有什么可以帮您？</h3>
            <p>输入问题，我将从知识库中检索相关内容为您解答</p>
          </div>
          <template v-else>
            <div v-for="msg in messageList" :key="msg.id" class="message-row" :class="msg.role">
              <div class="avatar" :class="msg.role">
                {{ msg.role === 'USER' ? '👤' : '🤖' }}
              </div>
              <div class="message-body">
                <div class="message-meta">
                  <span class="role-name">{{ msg.role === 'USER' ? '我' : 'AI 助手' }}</span>
                </div>
                <div v-if="msg.role === 'USER'" class="message-content user-text">{{ msg.content }}</div>
                <div v-else-if="msg.id === streamingMsgId && thinking" class="message-content ai-text thinking-bubble">
                  <div class="thinking-dots">
                    <span></span><span></span><span></span>
                  </div>
                  <span class="thinking-text">AI 正在思考中</span>
                </div>
                <div v-else class="message-content ai-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <span v-if="msg.role === 'ASSISTANT' && sending && !thinking && msg.id === streamingMsgId" class="typing-cursor"></span>
                <div v-if="msg.role === 'ASSISTANT' && msg.id && !sending" class="feedback-bar">
                  <button
                    class="feedback-btn"
                    :class="{ active: msg.feedback === 'LIKE' }"
                    title="有帮助"
                    @click="handleFeedback(msg, 'LIKE')"
                  >👍</button>
                  <button
                    class="feedback-btn"
                    :class="{ active: msg.feedback === 'DISLIKE' }"
                    title="没帮助"
                    @click="handleFeedback(msg, 'DISLIKE')"
                  >👎</button>
                </div>
                <div v-if="msg.referenceContent" class="source-refs">
                  <span class="source-refs-label">📚 信息源：</span>
                  <el-tag
                    v-for="(src, idx) in parseSources(msg.referenceContent)"
                    :key="idx"
                    class="source-tag"
                    size="small"
                    effect="plain"
                    @click="openSourceDrawer(src)"
                  >{{ src.docName }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="input-area" v-if="currentConversation">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 1, maxRows: 5 }"
              placeholder="输入你的问题，Ctrl+Enter 发送…"
              resize="none"
              @keydown.enter.ctrl="handleSend"
            />
            <el-button
              class="send-btn"
              type="primary"
              :icon="Promotion"
              :disabled="!inputText.trim() || sending"
              :loading="sending"
              @click="handleSend"
            >{{ sending ? '生成中' : '发送' }}</el-button>
          </div>
          <!-- <div class="input-tip">
            <span>RAG检索 + 工具调用 + 持久记忆</span>
          </div> -->
        </div>
      </div>
    </div>
    <el-drawer v-model="sourceDrawerVisible" :title="sourceDrawerTitle" direction="rtl" size="40%">
      <div class="source-drawer-content">{{ sourceDrawerContent }}</div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Plus, Delete, Promotion } from '@element-plus/icons-vue'
import { getConversationList, createConversation, deleteConversation, getMessages } from '@/api/conversation'
import { getDocumentList, getDocumentContent } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import router from '@/router'

marked.setOptions({ breaks: true, gfm: true })

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}

const userStore = useUserStore()
const conversationList = ref([])
const currentConversation = ref(null)
const messageList = ref([])
const inputText = ref('')
const messageAreaRef = ref()
const sending = ref(false)
const thinking = ref(false)
const streamingMsgId = ref(null)
const sourceDrawerVisible = ref(false)
const sourceDrawerTitle = ref('')
const sourceDrawerContent = ref('')

const parseSources = (refContent) => {
  if (!refContent) return []
  const all = refContent.split('\n===\n').map(block => {
    const lines = block.trim().split('\n')
    const header = lines[0] || ''
    const docMatch = header.match(/\[来源:(.+?)\s*\|/)
    return {
      docName: docMatch ? docMatch[1].trim() : '未知来源',
      content: lines.slice(1).join('\n').trim()
    }
  }).filter(s => s.content)
  // 按文档名去重，同名文档内容合并
  const map = new Map()
  all.forEach(s => {
    if (map.has(s.docName)) {
      map.get(s.docName).content += '\n\n---\n\n' + s.content
    } else {
      map.set(s.docName, { ...s })
    }
  })
  return Array.from(map.values())
}

const openSourceDrawer = async (src) => {
  sourceDrawerTitle.value = src.docName
  sourceDrawerContent.value = '加载中...'
  sourceDrawerVisible.value = true
  try {
    const listRes = await getDocumentList()
    const docs = listRes.data || []
    const doc = docs.find(d => d.documentName === src.docName)
    if (doc) {
      const contentRes = await getDocumentContent(doc.id)
      sourceDrawerContent.value = contentRes.data?.content || '文档内容为空'
    } else {
      sourceDrawerContent.value = '未找到该文档'
    }
  } catch (e) {
    sourceDrawerContent.value = '加载失败: ' + (e.message || '未知错误')
  }
}

const fetchConversations = async () => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return
    const res = await getConversationList(userId)
    conversationList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const selectConversation = async (conv) => {
  currentConversation.value = conv
  try {
    const res = await getMessages(conv.id)
    messageList.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error(e)
  }
}

const handleNewConversation = async () => {
  try {
    const userId = userStore.userInfo?.id
    const res = await createConversation({
      title: '新会话',
      businessType: 'QA',
      userId
    })
    const conv = res.data
    conversationList.value.unshift(conv)
    selectConversation(conv)
    ElMessage.success('会话已创建')
  } catch (e) {
    console.error(e)
  }
}

const handleDeleteConversation = (conv) => {
  ElMessageBox.confirm('确定要删除该会话及其所有消息吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteConversation(conv.id)
    conversationList.value = conversationList.value.filter(c => c.id !== conv.id)
    if (currentConversation.value?.id === conv.id) {
      currentConversation.value = null
      messageList.value = []
    }
    ElMessage.success('删除成功')
  })
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || !currentConversation.value || sending.value) return

  const userMsg = { role: 'USER', content: text, id: Date.now() }
  messageList.value.push(userMsg)
  inputText.value = ''
  sending.value = true
  await nextTick()
  scrollToBottom()

  const aiReplyId = Date.now() + 1
  const aiReply = {
    role: 'ASSISTANT',
    content: '',
    id: aiReplyId
  }
  messageList.value.push(aiReply)
  streamingMsgId.value = aiReplyId
  thinking.value = true
  await nextTick()
  scrollToBottom()

  try {
    const authToken = localStorage.getItem('token')
    const response = await fetch('/api/ai/chat/agent/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {})
      },
      body: JSON.stringify({
        conversationId: currentConversation.value.id,
        question: text
      })
    })

    if (!response.ok) {
      let errorMessage = '请求失败'
      try {
        const errorData = await response.json()
        errorMessage = errorData.message || errorMessage
      } catch (e) {
        if (response.status === 401) {
          errorMessage = '登录已过期，请重新登录'
        }
      }
      const error = new Error(errorMessage)
      error.status = response.status
      throw error
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop()

      for (const part of parts) {
        const lines = part.split('\n')
        let eventName = ''
        let dataLines = []
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5))
          }
        }
        if (eventName === 'done' || eventName === 'error') continue
        if (dataLines.length === 0) continue

        let tokenText = dataLines.join('\n')
        try { tokenText = JSON.parse(tokenText) } catch (e) { /* use raw */ }

        if (thinking.value) thinking.value = false
        const idx = messageList.value.findIndex(m => m.id === aiReply.id)
        if (idx !== -1) {
          messageList.value[idx].content += tokenText
        }
      }

      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    if (e.status === 401) {
      await userStore.logout()
      ElMessage.error(e.message || '登录已过期，请重新登录')
      router.push('/login')
    }
    const idx = messageList.value.findIndex(m => m.id === aiReply.id)
    if (idx !== -1 && !messageList.value[idx].content) {
      if (e.status === 401) {
        messageList.value.splice(idx, 1)
      } else {
        messageList.value[idx].content = 'AI 调用失败：' + (e.message || '请检查后端服务和 API Key 配置')
      }
    }
  } finally {
    sending.value = false
    thinking.value = false
    streamingMsgId.value = null
    // 重新加载消息列表，用真实数据库ID替换临时ID（确保点赞等操作能正确关联）
    try {
      const res = await getMessages(currentConversation.value.id)
      messageList.value = res.data || []
    } catch (ignored) {}
  }

  await nextTick()
  scrollToBottom()
}

const handleFeedback = async (msg, type) => {
  const newFeedback = msg.feedback === type ? null : type
  try {
    const token = userStore.token
    await fetch(`/api/ai/chat/message/${msg.id}/feedback`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({ feedback: newFeedback })
    })
    const idx = messageList.value.findIndex(m => m.id === msg.id)
    if (idx !== -1) {
      messageList.value[idx].feedback = newFeedback
    }
  } catch (e) {
    console.error(e)
  }
}

const scrollToBottom = () => {
  if (messageAreaRef.value) {
    messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
  }
}


onMounted(() => {
  fetchConversations()
})
</script>

<style lang="scss" scoped>
.chat-container {
  height: calc(100vh - 50px);
  margin: -20px;
  padding: 0;

  .chat-layout {
    height: 100%;
    display: flex;
    gap: 0;
  }

  .session-panel {
    width: 260px;
    min-width: 260px;
    background: #fff;
    border-radius: 0;
    box-shadow: none;
    border-right: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .panel-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #f0f0f0;

      .panel-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }
    }

    .session-list {
      flex: 1;
      overflow-y: auto;

      .session-item {
        padding: 12px 16px;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 10px;
        border-bottom: 1px solid #f8f8f8;
        position: relative;
        transition: background 0.2s;

        &:hover { background: #f5f7fa; }

        &.active {
          background: linear-gradient(135deg, #ecf5ff, #e8f0fe);
          border-left: 3px solid #409eff;
        }

        .session-icon { font-size: 18px; }

        .session-info {
          flex: 1;
          min-width: 0;

          .session-title {
            font-size: 13px;
            color: #303133;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .session-time {
            font-size: 11px;
            color: #c0c4cc;
            margin-top: 2px;
          }
        }

        .session-delete {
          opacity: 0;
          transition: opacity 0.2s;
        }

        &:hover .session-delete { opacity: 1; }
      }

      .empty-tip {
        padding: 40px 16px;
        text-align: center;
        color: #c0c4cc;
        font-size: 13px;
      }
    }
  }

  .chat-panel {
    flex: 1;
    background: #fff;
    border-radius: 0;
    box-shadow: none;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .chat-header {
      padding: 14px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #f0f0f0;

      .chat-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #303133;

        .title-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #67c23a;
          display: inline-block;
        }
      }
    }

    .message-area {
      flex: 1;
      overflow-y: auto;
      padding: 20px 24px;
      background: #fafbfc;

      .welcome-screen {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #909399;

        .welcome-icon {
          font-size: 48px;
          margin-bottom: 16px;
        }

        h3 {
          font-size: 18px;
          color: #303133;
          margin-bottom: 8px;
        }

        p {
          font-size: 14px;
          margin-bottom: 20px;
        }
      }

      .message-row {
        display: flex;
        gap: 12px;
        margin-bottom: 24px;

        &.USER {
          flex-direction: row-reverse;

          .message-body {
            align-items: flex-end;
          }

          .user-text {
            background: linear-gradient(135deg, #409eff, #337ecc);
            color: #fff;
            border-radius: 16px 4px 16px 16px;
            padding: 10px 16px;
          }
        }

        &.ASSISTANT {
          .ai-text {
            background: #fff;
            border: 1px solid #e8e8e8;
            border-radius: 4px 16px 16px 16px;
            padding: 14px 18px;
          }
        }

        .avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
          flex-shrink: 0;

          &.USER { background: #ecf5ff; }
          &.ASSISTANT { background: #f0f9eb; }
        }

        .message-body {
          display: flex;
          flex-direction: column;
          max-width: 75%;
          min-width: 0;

          .message-meta {
            margin-bottom: 4px;

            .role-name {
              font-size: 12px;
              color: #909399;
            }
          }

          .message-content {
            font-size: 14px;
            line-height: 1.7;
            word-break: break-word;
          }

          .feedback-bar {
            margin-top: 6px;
            display: flex;
            gap: 4px;

            .feedback-btn {
              background: none;
              border: 1px solid #e4e7ed;
              border-radius: 4px;
              cursor: pointer;
              font-size: 14px;
              padding: 2px 8px;
              opacity: 0.5;
              transition: all 0.15s;

              &:hover { opacity: 1; border-color: #c0c4cc; }
              &.active { opacity: 1; border-color: #409eff; background: #ecf5ff; }
            }
          }

          .message-ref {
            margin-top: 8px;
            background: #f8fafc;
            border: 1px solid #ebeef5;
            border-radius: 8px;
            padding: 10px 14px;

            .ref-label {
              font-size: 12px;
              color: #909399;
              margin-bottom: 6px;
              font-weight: 600;
            }

            .ref-text {
              font-size: 13px;
              color: #606266;
              line-height: 1.6;
              white-space: pre-wrap;
            }
          }
        }
      }
    }

    .input-area {
      padding: 16px 20px;
      border-top: 1px solid #f0f0f0;
      background: #fff;

      .input-wrapper {
        display: flex;
        gap: 12px;
        align-items: flex-end;

        :deep(.el-textarea__inner) {
          border-radius: 10px;
          padding: 10px 14px;
          font-size: 14px;
          box-shadow: none;
          border: 1px solid #dcdfe6;

          &:focus { border-color: #409eff; }
        }

        .send-btn {
          border-radius: 10px;
          height: 40px;
          min-width: 90px;
        }
      }

      .input-tip {
        font-size: 11px;
        color: #c0c4cc;
        margin-top: 6px;
        padding-left: 2px;
      }
    }
  }
}

.source-refs {
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;

  .source-refs-label {
    font-size: 12px;
    color: #909399;
  }

  .source-tag {
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      color: #409eff;
      border-color: #409eff;
      background: #ecf5ff;
    }
  }
}

.source-drawer-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  padding: 0 4px;
}

.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px 16px 16px 16px;
  padding: 14px 18px;

  .thinking-dots {
    display: flex;
    gap: 4px;

    span {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #409eff;
      animation: thinking-bounce 1.4s infinite ease-in-out;

      &:nth-child(1) { animation-delay: 0s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }

  .thinking-text {
    font-size: 13px;
    color: #909399;
  }
}

@keyframes thinking-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #409eff;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

:deep(.markdown-body) {
  h1, h2, h3, h4 {
    margin: 12px 0 8px;
    font-weight: 600;
    color: #303133;
  }
  h1 { font-size: 18px; }
  h2 { font-size: 16px; }
  h3 { font-size: 15px; }

  p {
    margin: 6px 0;
    line-height: 1.7;
    color: #303133;
  }

  ul, ol {
    padding-left: 20px;
    margin: 6px 0;

    li {
      margin: 4px 0;
      line-height: 1.7;
    }
  }

  code {
    background: #f0f2f5;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    color: #c7254e;
    font-family: Consolas, Monaco, monospace;
  }

  pre {
    background: #1e1e1e;
    border-radius: 8px;
    padding: 14px 16px;
    margin: 8px 0;
    overflow-x: auto;

    code {
      background: transparent;
      color: #d4d4d4;
      padding: 0;
      font-size: 13px;
    }
  }

  blockquote {
    border-left: 3px solid #409eff;
    padding: 8px 14px;
    margin: 8px 0;
    background: #f5f7fa;
    color: #606266;
    border-radius: 0 6px 6px 0;
  }

  table {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;

    th, td {
      border: 1px solid #ebeef5;
      padding: 8px 12px;
      font-size: 13px;
    }

    th {
      background: #f5f7fa;
      font-weight: 600;
    }
  }

  strong { color: #303133; }

  a {
    color: #409eff;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }

  hr {
    border: none;
    border-top: 1px solid #ebeef5;
    margin: 12px 0;
  }
}
</style>
