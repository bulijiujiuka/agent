# AI 知识工单平台

基于 **LangChain4j + Spring Boot + Vue 3** 的企业级智能客服与知识管理系统，集成 RAG 检索增强生成、AI Agent 工具调用、工单智能辅助三大核心能力。


![alt text](image.png)

![alt text](image-1.png)

![alt text](image-2.png)
## 核心亮点

- **Agent 两阶段流式架构** — Phase 1 同步执行工具调用收集上下文，Phase 2 通过 SSE 逐 token 流式生成，解决 LLM 工具调用与流式输出不兼容的工程难题
- **RAG-Tool 统一调度** — 将知识库向量检索封装为 `@Tool`，与工单查询、系统统计等工具统一编排，消除 RAG/Agent 模式割裂
- **知识库全链路** — 文档上传 → 文本切片(500字/片) → ONNX 本地向量化 → 语义检索 → Prompt 注入
- **持久化对话记忆** — 自定义 `ChatMemoryStore` 实现，自动过滤工具中间消息，支持多轮上下文连续对话
- **工单 AI 辅助** — 自动摘要、智能回复建议、多字段关键词检索

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3 + Element Plus + Vite | 组合式 API、SSE 流式接收 |
| **状态管理** | Pinia | 会话状态、用户信息 |
| **后端** | Spring Boot 3.2 + Java 17 | RESTful API |
| **ORM** | MyBatis | 注解式 SQL |
| **数据库** | MySQL | 业务数据持久化 |
| **AI 框架** | LangChain4j 0.35.0 | Agent、Tool、Memory |
| **LLM** | OpenAI 兼容 API | 支持多模型动态切换 |
| **向量化** | AllMiniLmL6V2 (ONNX) / 远程 API | 支持本地模型与远程中文 Embedding 模型切换 |
| **向量库** | InMemoryEmbeddingStore | 内存向量检索 |
| **缓存** | Caffeine | 热点问题缓存，30分钟过期 |
| **认证** | JWT | 无状态 Token 认证 |

## 项目结构

```
├── backend/                          # 后端 (Spring Boot)
│   ├── src/main/java/com/admin/
│   │   ├── config/                   # AiModelConfig, AiAssistantConfig, WebMvc
│   │   ├── controller/               # AiChatController (Agent流式), TicketController...
│   │   ├── dto/                      # 请求/响应 DTO
│   │   ├── entity/                   # Ticket, KbDocument, AiMessage, AiConversation...
│   │   ├── mapper/                   # MyBatis Mapper (注解式SQL)
│   │   └── service/
│   │       ├── AiChatService.java    # RAG 检索 + 流式问答
│   │       ├── AiToolService.java    # 7个 @Tool 工具方法
│   │       ├── PersistentChatMemoryStore.java  # 持久化对话记忆
│   │       ├── KbDocumentService.java          # 文档解析与切片
│   │       └── TicketService.java              # 工单AI辅助
│   ├── src/main/resources/
│   │   ├── application.yml           # 配置 (敏感值引用 .env)
│   │   └── sql/ai_business.sql       # 数据库建表脚本
│   ├── .env                          # 敏感配置 (git忽略)
│   └── .env.example                  # 配置模板
│
├── frontend/                         # 前端 (Vue 3)
│   └── src/
│       ├── api/                      # conversation, ticket, knowledge...
│       ├── layout/                   # 侧边栏 + 顶栏布局
│       └── views/
│           ├── ai/chat/              # AI 问答 (SSE流式)
│           ├── ai/prompt/            # Prompt 配置管理
│           ├── ai/model/             # 模型管理
│           ├── ai/stats/             # 评测看板
│           ├── ai/audit/             # 会话审计
│           ├── ai/observability/     # 可观测性
│           ├── knowledge/            # 知识库文档管理 + 分类
│           ├── ticket/               # 工单辅助
│           └── system/               # 用户、角色、文件、日志、敏感词
│
└── sample_docs/                      # 示例知识库文档 (8篇)
```

## 功能模块

| 模块 | 功能 |
|------|------|
| **AI 问答** | Agent 流式对话、工具自动调用、知识库检索、多轮记忆、热点缓存 |
| **知识库管理** | 文档上传(TXT/MD/CSV)、文本切片、向量索引、版本管理、启用/禁用 |
| **工单辅助** | AI 自动摘要、智能回复建议、多字段搜索(标题/客户名/内容/工单号) |
| **Prompt 配置** | 系统提示词、格式模板、上下文模板，在线编辑即时生效 |
| **模型管理** | 多模型配置、动态切换、默认模型设置 |
| **评测看板** | 响应时间统计、Token 用量、满意度分析 |
| **会话审计** | 对话历史回溯、消息检索 |
| **可观测性** | AI 调用链日志追踪 (traceId → Embedding → Retrieval → LLM) |
| **敏感词过滤** | 输入输出双向过滤 |
| **系统管理** | 用户管理、角色管理、文件管理、操作日志 |

## Agent 工具列表

| 工具 | 说明 |
|------|------|
| `searchTickets` | 按关键词搜索工单 (标题/客户名/内容/工单号) |
| `queryTicketByNo` | 按工单号查询详情 |
| `searchKnowledge` | 知识库语义检索 |
| `listKnowledgeDocuments` | 查询知识库文档列表 |
| `getSystemStats` | 获取系统统计数据 |
| `getCurrentTime` | 获取当前时间 |
| `calculateDays` | 日期间隔计算 |

## 快速开始

### 环境要求

- **Java** >= 17
- **Maven** >= 3.8
- **Node.js** >= 18
- **MySQL** >= 8.0

### 1. 初始化数据库

```sql
CREATE DATABASE reg_zhishiku DEFAULT CHARACTER SET utf8mb4;
-- 导入建表脚本
source backend/src/main/resources/sql/ai_business.sql;
```

### 2. 配置环境变量

```bash
cd backend
cp .env.example .env
# 编辑 .env，填入实际的数据库密码、AI API Key 等
```

`.env` 文件示例：

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=reg_zhishiku
DB_USERNAME=root
DB_PASSWORD=your_password

JWT_SECRET=your_jwt_secret_base64

AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=your_api_key
AI_MODEL_NAME=gpt-4o

# Embedding 模型配置（local=本地ONNX模型, api=远程API模型）
EMBEDDING_TYPE=local
EMBEDDING_BASE_URL=
EMBEDDING_API_KEY=
EMBEDDING_MODEL_NAME=text-embedding-v1
```

> **说明**：`EMBEDDING_TYPE=api` 时，若 `EMBEDDING_BASE_URL` / `EMBEDDING_API_KEY` 留空，会自动复用 `AI_BASE_URL` / `AI_API_KEY`。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务：http://localhost:8080

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务：http://localhost:3000

### 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |
| user | user123 | 普通用户 |

## 架构设计

### Agent 三阶段流式架构

```
用户提问
  │
  ▼
Phase 1: 同步工具调用循环 (ChatLanguageModel)
  │  ├─ LLM 判断是否需要工具 → 调用 searchTickets / searchKnowledge ...
  │  ├─ 收集工具返回文本 → toolContext
  │  └─ 最多循环 3 轮
  │
  ▼
Phase 1.5: 强制知识库兜底检索
  │  ├─ 若 LLM 未调用知识库工具或调用后未命中
  │  ├─ 强制执行一次 searchKnowledge 确保知识库被检索
  │  └─ 命中则补充到 toolContext，未命中则返回固定答复
  │
  ▼
Phase 2: 流式生成 (StreamingChatLanguageModel + SSE)
  │  ├─ 构建消息: history + SystemMessage(toolContext) + UserMessage
  │  ├─ 逐 token 通过 SSE 推送到前端
  │  └─ 完成后保存 USER + ASSISTANT 到持久化记忆
  │
  ▼
前端实时渲染 Markdown（含思考中动效）
```

### 知识库未命中 Guardrail

当知识库中无法检索到与用户问题相关的内容时，系统会：

1. **拦截 LLM 自由发挥** — 不生成基于模型自身知识的回答，避免编造公司内部制度、流程、数据
2. **返回固定引导答复** — 明确告知用户知识库未命中，并提供下一步建议
3. **覆盖所有链路** — Agent 流式、普通聊天、Service 层流式三条链路均生效

### 知识库 RAG 流程

```
文档上传 → 文本切片(500字/片, 50字重叠) → Embedding(本地ONNX或远程API) → InMemory 向量库
                                                                              │
用户提问 → Embedding → 向量相似度检索(Top5, minScore=0.5) → Guardrail判断 → Prompt 注入 → LLM 生成
```

## API 接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AI 对话 | POST | `/api/ai/chat/agent/stream` | Agent 流式问答 (SSE) |
| AI 对话 | POST | `/api/ai/chat/stream` | RAG 流式问答 (SSE) |
| 会话 | GET | `/api/ai/conversation/list` | 会话列表 |
| 会话 | POST | `/api/ai/conversation` | 新建会话 |
| 知识库 | GET | `/api/kb/document/list` | 文档列表 |
| 知识库 | POST | `/api/kb/document/upload` | 上传文档 |
| 工单 | GET | `/api/ticket/list` | 工单列表 |
| 工单 | POST | `/api/ticket/{id}/ai-assist` | AI 辅助处理 |
| 认证 | POST | `/api/auth/login` | 登录 |

## Docker 部署

```bash
# 1. 复制环境变量配置
cp backend/.env.example backend/.env
# 编辑 backend/.env，填入实际值

# 2. 一键构建并启动
docker compose up -d --build

# 3. 查看后端日志
docker compose logs -f backend

# 4. 前端访问
# http://your-server-ip:3313
```

## 配置说明

所有敏感配置通过 `.env` 文件管理，由 `spring-dotenv` 自动加载。`application.yml` 中使用 `${env.VAR}` 引用。

`.env` 文件已被 `.gitignore` 排除，不会提交到仓库。新环境部署时复制 `.env.example` 并填入实际值即可。

### Embedding 模型切换

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `EMBEDDING_TYPE` | `local` 本地ONNX模型 / `api` 远程API模型 | `local` |
| `EMBEDDING_BASE_URL` | 远程 Embedding API 地址（留空复用 `AI_BASE_URL`） | 空 |
| `EMBEDDING_API_KEY` | 远程 Embedding API Key（留空复用 `AI_API_KEY`） | 空 |
| `EMBEDDING_MODEL_NAME` | 远程模型名称 | `text-embedding-v1` |

启动日志会打印当前使用的 Embedding 模型类型，便于确认配置是否生效。

## License

MIT
