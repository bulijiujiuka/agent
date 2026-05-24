# AI 工单辅助平台技术亮点与评测报告

生成时间：2026-05-22  
项目路径：`D:\workspace\zhinenggongdanxitong`

## 1. 项目概览

本项目是一个面向企业客服与知识管理场景的 AI 工单辅助平台，后端基于 Spring Boot + MyBatis + MySQL 构建用户、工单、知识库、AI 会话、模型配置、Prompt 配置、审计统计等能力；AI 层基于 LangChain4j 接入大模型、Embedding、Agent Tool、ChatMemoryStore 和 Milvus 向量库；前端基于 Vue 3 + Element Plus 实现知识库管理、工单管理、AI 对话、模型/Prompt 配置、会话审计和可观测页面。

当前数据库规模：

| 数据表 | 当前数量 |
|---|---:|
| ticket | 2 |
| kb_document | 10 |
| kb_chunk | 32 |
| ai_message | 129 |
| ai_chat_memory | 54 |
| ai_call_log | 12 |

说明：当前评测数据为自建评测集，不是 200+ 真实线上工单数据。

## 2. 核心技术亮点

### 2.1 RAG 知识库全链路

代码位置：

- `backend/src/main/java/com/admin/service/KbDocumentService.java`
- `backend/src/main/java/com/admin/service/SemanticChunkService.java`
- `backend/src/main/java/com/admin/config/AiModelConfig.java`
- `backend/src/main/java/com/admin/service/KnowledgeRetrievalService.java`

亮点总结：

- 支持文档上传、解析、语义切片、向量化、索引重建、删除、启停和版本快照。
- 切片阶段保留 `sectionTitle`、`titlePath`，为后续上下文引用和邻近片段扩展提供元数据。
- 切片向量化过程维护 `kb_chunk.vector_status`，支持 `PENDING / PROCESSING / COMPLETED / FAILED` 状态跟踪。
- 向量元数据写入 `documentId`、`documentName`、`chunkId`、`chunkIndex`、`category`、`sectionTitle`、`titlePath`、`version`，便于过滤、溯源和删除。
- 支持 `REBUILD_ALL` 与 `MISSING_ONLY` 两种索引任务模式，避免每次启动都重复重建。
- 文档删除、重传、禁用时会清理对应 Milvus 向量，避免重复向量和旧版本污染检索结果。

可写简历表达：

> 完善知识库文档解析、语义切片、向量化、重建、删除与启停的索引生命周期，修复重复向量和 chunk 状态不一致问题，提升知识库更新可靠性。

### 2.2 Milvus 向量库接入

代码位置：

- `backend/src/main/java/com/admin/config/AiModelConfig.java`
- `backend/src/main/resources/application.yml`

亮点总结：

- 将向量存储从内存模式扩展为 Milvus，配置项支持 `VECTOR_STORE_TYPE=milvus`。
- Milvus collection、host、port、dimension 均通过环境变量配置。
- Embedding 同时支持本地 AllMiniLmL6V2 和远程 OpenAI-compatible API。
- 当前配置默认 collection 为 `kb_chunks`，dimension 为 `1024`。

可写简历表达：

> 基于 LangChain4j 接入 Milvus 向量库，支持本地/远程 Embedding 切换，并通过元数据过滤实现文档级检索与索引清理。

### 2.3 混合检索召回优化

代码位置：

- `backend/src/main/java/com/admin/service/KnowledgeRetrievalService.java`
- `backend/src/main/java/com/admin/mapper/KbChunkMapper.java`
- `backend/src/main/java/com/admin/dto/KnowledgeSearchHit.java`

亮点总结：

- 实现向量检索 + MySQL FULLTEXT 检索的混合召回。
- FULLTEXT 不可用或无结果时自动 fallback 到 LIKE 检索。
- 通过 `vectorWeight=0.65`、`keywordWeight=0.35` 做归一化加权融合。
- 支持 `vectorTopK`、`keywordTopK`、`finalTopK`、`minVectorScore` 参数化配置。
- 命中主 chunk 后扩展前后邻近片段，缓解单个 chunk 上下文不完整问题。
- 检索测试接口支持 `VECTOR / HYBRID` 模式、是否扩展邻近片段、是否启用缓存、是否查询重写，方便做消融对比。

评测支撑：

- 在 100 条自建评测集上，整体 Hit@1/拒答成功由 `74.00%` 提升到 `79.00%`。
- 在可回答问题子集上，Hit@1 由 `86.05%` 提升到 `91.86%`，MRR 由 `0.91` 提升到 `0.95`。

可写简历表达：

> 引入向量检索 + 关键词检索 + 邻近片段扩展的混合召回策略，在 100 条自建评测集上整体 Hit@1 由 74.0% 提升至 79.0%。

### 2.4 多轮问答与查询重写

代码位置：

- `backend/src/main/java/com/admin/service/AiChatService.java`
- `backend/src/main/java/com/admin/service/PersistentChatMemoryStore.java`
- `backend/src/main/java/com/admin/mapper/AiConversationMapper.java`
- `backend/src/main/java/com/admin/mapper/AiMessageMapper.java`

亮点总结：

- 自定义 `PersistentChatMemoryStore`，将 LangChain4j 对话记忆持久化到 MySQL。
- 普通 RAG 对话通过 `ai_message` 保存完整历史，Agent 对话通过 `ai_chat_memory` 保存 LangChain4j memory。
- 长对话触发摘要机制：超过阈值后保留近期消息，并把更早消息压缩为 conversation summary。
- 检索前根据历史摘要 + 最近消息进行查询重写，补全“那这个呢”“超过多少要审批”等追问语义。
- 查询重写支持开关：`QUERY_REWRITE_ENABLED=true/false`。

评测支撑：

- 在 8 条自建追问 case、3 轮共 24 个样本上，HYBRID_REWRITE 的追问 Hit@1 为 `100%`。
- 但追问样本较小，不建议把 `37.5% -> 100%` 作为主指标；更适合用作功能验证或面试时补充说明。

可写简历表达：

> 设计 MySQL 持久化会话记忆 + 查询重写机制，补全连续追问语义，提升多轮问答下的上下文理解与检索稳定性。

### 2.5 Agent Tool 与两阶段流式架构

代码位置：

- `backend/src/main/java/com/admin/controller/AiChatController.java`
- `backend/src/main/java/com/admin/service/AiToolService.java`
- `backend/src/main/java/com/admin/config/AiAssistantConfig.java`
- `frontend/src/views/ai/chat/index.vue`

亮点总结：

- 封装 7 个 Agent Tool：
  - `queryTicketByNo`
  - `searchTickets`
  - `listKnowledgeDocuments`
  - `queryDocumentById`
  - `querySystemStats`
  - `searchKnowledge`
  - `searchDocumentContent`
- Agent 流式接口采用两阶段架构：
  - Phase 1：同步执行工具调用，收集工单、知识库、系统统计等上下文。
  - Phase 1.5：工具未调用知识库或未命中时，强制执行一次知识库兜底检索。
  - Phase 2：将工具上下文注入 SystemMessage，再通过 SSE token 级流式输出。
- 解决了 Tool Calling 与 Streaming 直接组合时上下文难收集、输出不连续的问题。
- 前端使用 `fetch + ReadableStream` 解析 SSE，并支持 Markdown 渲染、思考中状态、来源引用抽屉和点赞/点踩反馈。

可写简历表达：

> 设计两阶段 Agent 流式架构，先同步执行工具调用收集业务上下文，再通过 SSE 实现 token 级流式输出，统一知识库检索、工单查询和系统统计能力。

### 2.6 Caffeine + Redis 二级缓存

代码位置：

- `backend/src/main/java/com/admin/service/TwoLevelCacheService.java`
- `backend/src/main/java/com/admin/service/KnowledgeRetrievalService.java`
- `backend/src/main/java/com/admin/service/AiToolService.java`
- `backend/src/main/java/com/admin/service/AiChatService.java`
- `backend/src/main/java/com/admin/service/KbDocumentService.java`

亮点总结：

- 使用 Caffeine 做本地缓存，Redis 做分布式缓存。
- 支持逻辑过期、空值缓存、异步刷新、分布式锁防击穿。
- 覆盖缓存场景：
  - RAG TopK 检索结果：`rag:topk:*`
  - 普通问答缓存：`qa:normal:*`
  - 流式问答缓存：`qa:stream:*`
  - Agent 问答缓存：`agent:*`
  - Prompt 配置缓存：`config:prompt:*`
  - 模型配置缓存：`config:model:*`
  - Tool 文档列表和系统统计缓存
- 知识库文档变化、索引重建、Prompt/模型配置变化时会清理相关缓存，降低旧答案污染风险。

评测支撑：

- 86 条可回答问题，3 轮共 258 次重复检索样本。
- 冷检索 p50：`163ms`，热缓存 p50：`0ms`。
- 冷检索 p95：`277ms`，热缓存 p95：`1ms`。
- 重复检索 Top 结果一致率：`100%`。

可写简历表达：

> 基于 Caffeine + Redis 构建二级缓存，覆盖热点检索结果；258 次重复检索评测中，检索层 p95 延迟由 277ms 降至 1ms。

### 2.7 AI 工单辅助

代码位置：

- `backend/src/main/java/com/admin/service/TicketService.java`
- `backend/src/main/java/com/admin/mapper/TicketMapper.java`
- `frontend/src/views/ticket/index.vue`

亮点总结：

- 工单创建后异步触发 AI 辅助生成。
- 根据工单标题和内容检索知识库上下文。
- 自动生成三类字段：
  - 问题摘要
  - 客户回复建议
  - 内部处理建议
- 工单搜索支持标题、客户名称、内容、工单编号多字段匹配。

可写简历表达：

> 实现工单 AI 辅助处理，根据工单内容检索知识库并自动生成问题摘要、客户回复建议和内部处理建议。

### 2.8 可观测与会话审计

代码位置：

- `backend/src/main/java/com/admin/service/AiCallLogService.java`
- `backend/src/main/java/com/admin/controller/AiAuditController.java`
- `backend/src/main/java/com/admin/controller/AiStatsController.java`
- `frontend/src/views/ai/audit/index.vue`
- `frontend/src/views/ai/observability/index.vue`
- `frontend/src/views/ai/stats/index.vue`

亮点总结：

- 每次 AI 调用生成 traceId。
- 记录 Embedding、Retrieval、LLM_CALL 等步骤耗时、输入输出长度、调用状态和错误信息。
- 支持会话审计、消息追溯、调用日志查询、反馈统计和满意度统计。
- AI 消息保存 `reference_content`、`retrieval_score`、`retrieval_hit_count`，便于追踪回答依据。

可写简历表达：

> 建设 AI 调用链路可观测能力，记录 Retrieval、LLM 调用耗时与命中信息，支持会话审计、反馈统计和问题追溯。

### 2.9 Prompt 与模型动态配置

代码位置：

- `backend/src/main/java/com/admin/controller/AiPromptConfigController.java`
- `backend/src/main/java/com/admin/controller/AiModelConfigController.java`
- `backend/src/main/java/com/admin/service/AiChatService.java`
- `frontend/src/views/ai/prompt/index.vue`
- `frontend/src/views/ai/model/index.vue`

亮点总结：

- 支持在线配置系统 Prompt、格式 Prompt、上下文模板、历史模板、问题模板。
- 支持多模型配置、默认模型、启用模型和前端模型选择。
- Prompt 和模型配置走二级缓存，更新后清理相关缓存。
- 流式问答支持按 `modelKey` 动态构建 StreamingChatLanguageModel。

可写简历表达：

> 实现 Prompt 与模型配置中心，支持在线调整问答模板、多模型切换和配置缓存失效，提升 AI 能力运维灵活性。

### 2.10 安全与管理能力

代码位置：

- `backend/src/main/java/com/admin/config/JwtInterceptor.java`
- `backend/src/main/java/com/admin/util/JwtUtil.java`
- `backend/src/main/java/com/admin/config/RateLimitInterceptor.java`
- `backend/src/main/java/com/admin/service/SensitiveWordService.java`
- `backend/src/main/java/com/admin/aspect/OperLogAspect.java`

亮点总结：

- JWT 登录认证和接口拦截。
- 用户、角色、文件、操作日志、敏感词管理。
- Caffeine 实现简单限流。
- 输入输出敏感词过滤。
- 操作日志 AOP 记录后台关键操作。

可写简历表达：

> 完成 JWT 鉴权、操作日志、敏感词过滤和基础限流能力，保障后台管理与 AI 对话链路的基础安全。

## 3. 评测体系

评测代码：

- `backend/src/test/java/com/admin/eval/RagEvalTest.java`
- `backend/src/test/java/com/admin/eval/RagCacheEvalTest.java`

评测数据与报告：

- `eval/rag_eval_cases.csv`
- `eval/reports/rag_eval_details.csv`
- `eval/reports/rag_eval_summary.json`
- `eval/reports/rag_cache_eval_details.csv`
- `eval/reports/rag_cache_eval_summary.json`

### 3.1 RAG 评测集构成

| 问题类型 | 条数 |
|---|---:|
| direct | 64 |
| synonym | 8 |
| fuzzy | 6 |
| followup | 8 |
| boundary | 6 |
| irrelevant | 8 |
| 合计 | 100 |

评测设置：

- 每轮跑 3 次。
- TopK = 5。
- 对比模式：`VECTOR`、`HYBRID`、`HYBRID_REWRITE`。
- 过滤 `source = NEIGHBOR` 的扩展片段，只统计真正排序结果。
- 检索缓存关闭，避免缓存影响召回和延迟。
- `HYBRID_REWRITE` 仅在 followup 问题上评测。

### 3.2 整体评测结果

`answerable Hit@K`：只统计知识库内可回答问题。  
`overall Hit@K/Reject`：把 boundary/irrelevant 也算进总样本，要求无关问题能拒答才算成功。

| 模式 | 样本数 | 可回答样本 | answerable Hit@1 | answerable Hit@5 | MRR | overall Hit@1/Reject | overall Hit@5/Reject | No-hit Accuracy | p50 | p95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| VECTOR | 300 | 258 | 86.05% | 98.84% | 0.91 | 74.00% | 85.00% | 0.00% | 154ms | 222ms |
| HYBRID | 300 | 258 | 91.86% | 98.84% | 0.95 | 79.00% | 85.00% | 0.00% | 152ms | 284ms |
| HYBRID_REWRITE | 24 | 24 | 100.00% | 100.00% | 1.00 | 100.00% | 100.00% | - | 1063ms | 2347ms |

结论：

- 混合检索相较纯向量检索，在可回答问题子集上 Hit@1 提升 `5.81` 个百分点，MRR 提升 `0.04`。
- 严格整体口径下，Hit@1/拒答成功由 `74.00%` 提升到 `79.00%`。
- 当前 No-hit Accuracy 为 `0.00%`，说明边界/无关问题仍会返回检索结果，后续应增加分数阈值、拒答分类或 LLM guardrail 评测。
- `HYBRID_REWRITE` 追问结果较好，但样本仅 8 条、3 轮 24 样本，不建议作为主指标单独强调。

### 3.3 分类型评测结果

| 模式/类型 | 样本数 | 可回答样本 | Hit@1 | Hit@5 | MRR | p50 | p95 |
|---|---:|---:|---:|---:|---:|---:|---:|
| VECTOR/direct | 192 | 192 | 92.19% | 100.00% | 0.95 | 155ms | 247ms |
| VECTOR/synonym | 24 | 24 | 75.00% | 100.00% | 0.88 | 154ms | 298ms |
| VECTOR/fuzzy | 18 | 18 | 100.00% | 100.00% | 1.00 | 145ms | 192ms |
| VECTOR/followup | 24 | 24 | 37.50% | 87.50% | 0.52 | 143ms | 203ms |
| VECTOR/boundary | 18 | 0 | - | - | - | 150ms | 189ms |
| VECTOR/irrelevant | 24 | 0 | - | - | - | 155ms | 193ms |
| HYBRID/direct | 192 | 192 | 100.00% | 100.00% | 1.00 | 152ms | 284ms |
| HYBRID/synonym | 24 | 24 | 75.00% | 100.00% | 0.88 | 149ms | 270ms |
| HYBRID/fuzzy | 18 | 18 | 100.00% | 100.00% | 1.00 | 149ms | 292ms |
| HYBRID/followup | 24 | 24 | 37.50% | 87.50% | 0.55 | 156ms | 272ms |
| HYBRID/boundary | 18 | 0 | - | - | - | 161ms | 413ms |
| HYBRID/irrelevant | 24 | 0 | - | - | - | 141ms | 214ms |
| HYBRID_REWRITE/followup | 24 | 24 | 100.00% | 100.00% | 1.00 | 1063ms | 2347ms |

结论：

- 混合检索对 direct 问题改善明显，Hit@1 从 `92.19%` 提升到 `100.00%`。
- synonym 问题仍为 `75.00%`，说明同义改写、业务词表或 BM25 分词仍可继续优化。
- followup 在不开启 rewrite 时效果较弱，说明多轮追问依赖查询重写是合理的。
- rewrite 延迟较高，p95 为 `2347ms`，这是因为重写本身需要额外 LLM 调用。

### 3.4 缓存评测结果

评测设置：

- 仅统计 86 条可回答问题。
- 每条问题先清理 `rag:topk:` 缓存，执行一次冷检索。
- 再立即重复执行同一问题，统计热缓存延迟。
- 3 轮共 258 个样本。

| 指标 | 冷检索 | 热缓存 | 降低比例 |
|---|---:|---:|---:|
| p50 latency | 163ms | 0ms | 100.00% |
| p95 latency | 277ms | 1ms | 99.64% |
| Top 结果一致率 | - | 100.00% | - |

分类型 p95：

| 类型 | 样本数 | 冷检索 p95 | 热缓存 p95 | 降低比例 |
|---|---:|---:|---:|---:|
| direct | 192 | 314ms | 1ms | 99.68% |
| synonym | 24 | 266ms | 1ms | 99.62% |
| fuzzy | 18 | 190ms | 1ms | 99.47% |
| followup | 24 | 208ms | 0ms | 100.00% |

结论：

- 二级缓存对重复检索/热点问题效果明显。
- 该指标只代表检索层缓存效果，不代表包含 LLM 生成的完整问答耗时。

## 4. 可用于简历的项目亮点版本

推荐写法：

> 【Agent】参与 AI 工单辅助与 RAG 问答系统建设，基于 Spring Boot、LangChain4j、Milvus、MySQL、SSE 实现知识库问答、工单辅助和 Agent Tool 调用能力。

> ● 召回优化：引入向量检索 + 关键词检索 + 邻近片段扩展的混合召回策略，在 100 条自建评测集上整体 Hit@1 由 74.0% 提升至 79.0%。  
> ● 缓存优化：基于 Caffeine + Redis 构建二级缓存，覆盖热点检索结果；258 次重复检索评测中，检索层 p95 延迟由 277ms 降至 1ms。  
> ● 多轮问答：设计 MySQL 持久化记忆 + 查询重写机制，补全连续追问语义，提升多轮问答下的上下文理解与检索稳定性。  
> ● 索引治理：完善文档解析、切片、向量化、重建、删除生命周期，修复重复向量与 chunk 状态不一致问题，提升知识库更新可靠性。  
> ● 流式 Agent：设计两阶段 Agent 流式架构，先同步执行工具调用收集上下文，再通过 SSE 实现 token 级流式输出。  
> ● 工单辅助：将工单查询、知识库检索、系统统计封装为 Agent Tool，并根据工单内容自动生成问题摘要、客户回复和内部处理建议。

如果想更强调可观测能力，可加一条：

> ● 可观测：记录 Retrieval、LLM 调用耗时、检索命中数和引用内容，支持会话审计、反馈统计和问题追溯。

## 5. 面试追问口径

### 5.1 关于评测集

可以这样回答：

> 当前指标来自 100 条自建 RAG 评测集，覆盖直接问、同义问、模糊问、追问、边界和无关问题。评测时关闭缓存，过滤邻近扩展片段，只统计真实排序结果。这个数据用于证明优化有效，不等同于线上真实工单指标；后续可以接入 200+ 真实工单/FAQ 继续复跑。

### 5.2 关于多轮追问指标

可以这样回答：

> 多轮追问目前样本较小，所以我没有把 `37.5% -> 100%` 作为主指标写在简历里。代码层面做的是 MySQL 持久化记忆、历史摘要和查询重写，追问子集验证说明链路有效，但正式量化还需要更大的真实追问数据集。

### 5.3 关于缓存指标

可以这样回答：

> 缓存指标统计的是检索层，不包含 LLM 生成耗时。评测方式是每条问题先清空 `rag:topk:` 做冷检索，再重复同一问题走热缓存，所以能体现热点问题和重复问题的检索耗时下降。

### 5.4 关于边界问题

可以这样回答：

> 当前边界/无关问题的拒答准确率还不理想，因为检索层只要有相似片段就会返回。后续可以增加 finalScore 阈值、查询分类器、LLM judge 或人工标注的拒答评测，把 No-hit Accuracy 作为单独优化目标。

## 6. 后续优化建议

1. 扩充 200+ 真实工单/FAQ 评测集，按 direct、synonym、fuzzy、followup、boundary、irrelevant 分层统计。
2. 为无关/边界问题增加拒答阈值，例如按 `finalScore`、Top1/Top2 分差、关键词命中情况组合判断。
3. 针对 synonym 类型补充业务词典、同义词改写或 query expansion。
4. 将查询重写从每次 LLM 调用优化为规则判断 + 只在追问场景触发，降低 p95 延迟。
5. 为二级缓存增加命中率计数器，区分本地 Caffeine 命中、Redis 命中、回源重建。
6. 将索引任务状态持久化到数据库，避免应用重启后任务状态丢失。
7. 为 Milvus collection 增加启动时健康检查和维度校验，避免 collection 配错导致检索为空。

