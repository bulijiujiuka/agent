package com.admin.service;

import com.admin.dto.KnowledgeSearchHit;
import com.admin.dto.RetrievalTestRequest;
import com.admin.dto.RetrievalTestResponse;
import com.admin.dto.RetrievalTestResult;
import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.entity.AiModelConfig;
import com.admin.entity.KbChunk;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.AiMessageMapper;
import com.admin.mapper.KbChunkMapper;
import com.admin.mapper.KbDocumentMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int RECENT_MEMORY_COUNT = 10;
    private static final int SUMMARY_TRIGGER_COUNT = 16;
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final com.admin.mapper.AiPromptConfigMapper promptConfigMapper;
    private final com.admin.mapper.AiModelConfigMapper modelConfigMapper;
    private final SensitiveWordService sensitiveWordService;
    private final AiCallLogService aiCallLogService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final TwoLevelCacheService cacheService;
    private final ShortTermMemoryService shortTermMemoryService;


    @Value("${kb.index.on-startup:false}")
    private boolean indexOnStartup;

    @Value("${kb.index.startup-mode:MISSING_ONLY}")
    private String startupIndexMode;

    @Value("${ai.rag.query-rewrite.enabled:true}")
    private boolean queryRewriteEnabled;

    private String rewriteQuestionForRetrieval(String question,String memoryText){
        if (!queryRewriteEnabled){
            return question;
        }

        if (memoryText == null || memoryText.isBlank()){
            return question;
        }

        try {
            String prompt = """
                    你是一个 RAG 检索查询改写器。
                    请根据历史对话，把用户当前问题改写成适合知识库检索的完整查询。
                    
                    要求：
                    1. 只输出改写后的查询文本，不要解释。
                    2. 如果当前问题已经完整清晰，直接原样输出。
                    3. 不要编造历史对话中没有的信息。
                    4. 保留关键实体、业务名词、时间、编号、文档名。
                    5. 输出中文。
                    
                    【历史上下文】
                    %s
     
                    【用户当前问题】
                    %s
                    """.formatted(memoryText,question);

            String rewiteenQuestion = chatModel.generate(prompt);

            if (rewiteenQuestion == null || rewiteenQuestion.isBlank()){
                return question;
            }

            rewiteenQuestion = rewiteenQuestion.trim();

            if (rewiteenQuestion.length() > 300){
                rewiteenQuestion = rewiteenQuestion.substring(0, 300);
            }

            log.info("查询重写 [{}] -> [{}]", question, rewiteenQuestion);
            return rewiteenQuestion;
        }catch (Exception e){
            log.warn("查询重写失败，使用原始问题", e);
            return question;
        }
    }

    //线程池
    private final ExecutorService indexExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kb-index-task");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private final Map<String, IndexTaskStatus> indexTasks = new ConcurrentHashMap<>();

    //任务状态类
    public record IndexTaskStatus(
            String taskId,
            String mode,
            String status,
            String message,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}

    //提交任务类
    public IndexTaskStatus startIndexTask(String mode) {
        if (!indexing.compareAndSet(false, true)) {
            throw new RuntimeException("已有索引任务正在执行，请稍后再试");
        }

        String taskId = UUID.randomUUID().toString();
        IndexTaskStatus started = new IndexTaskStatus(
                taskId,
                mode,
                "RUNNING",
                "索引任务已开始",
                LocalDateTime.now(),
                null
        );
        indexTasks.put(taskId, started);

        indexExecutor.submit(() -> {
            try {
                indexAllChunks(mode);

                indexTasks.put(taskId, new IndexTaskStatus(
                        taskId,
                        mode,
                        "COMPLETED",
                        "索引任务已完成",
                        started.startTime(),
                        LocalDateTime.now()
                ));
            } catch (Exception e) {
                log.error("索引任务失败: {}", e.getMessage(),e);

                indexTasks.put(taskId, new IndexTaskStatus(
                        taskId,
                        mode,
                        "FAILED",
                        e.getMessage(),
                        started.startTime(),
                        LocalDateTime.now()
                ));
            }
            finally {
                indexing.set(false);
            }
        });
        return started;
    }

    public IndexTaskStatus getIndexTask(String taskId) {
        IndexTaskStatus status = indexTasks.get(taskId);
        if (status == null) {
            throw new RuntimeException("索引任务不存在"+taskId);
        }
        return status;
    }

    @PreDestroy
    public void shutdownIndexExecutor() {
        indexExecutor.shutdown();
    }

    private StreamingChatLanguageModel buildStreamingModel(com.admin.entity.AiModelConfig mc) {
        return dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                .baseUrl(mc.getBaseUrl()).apiKey(mc.getApiKey()).modelName(mc.getModelId())
                .temperature(mc.getTemperature()).build();
    }

    private com.admin.entity.AiModelConfig resolveModel(String modelKey) {
        String key = modelKey == null || modelKey.isEmpty() ? "default" : modelKey;
        return cacheService.get(
                "config:model:" + key,
                new TypeReference<AiModelConfig>() {},
                Duration.ofMinutes(10),
                Duration.ofMinutes(1),
                () -> {
                    if (modelKey != null && !modelKey.isEmpty()) {
                        var mc = modelConfigMapper.findByKey(modelKey);
                        if (mc != null && mc.getEnabled()) return mc;
                    }
                    return modelConfigMapper.findDefault();
                }
        );
    }

    @PostConstruct
    public void indexAllChunksAsync() {
        if (!indexOnStartup) {
            log.info("启动时自动构建知识库向量索引已关闭");
            return;
        }

        new Thread(() -> {
            try {
                log.info("开始异步构建知识库向量索引...");
                indexAllChunks();
            } catch (Exception e) {
                log.error("异步构建向量索引失败: {}", e.getMessage());
            }
        }, "kb-index-thread").start();
    }

    private void evictKnowledgeCaches() {
        // 检索 TopK 结果依赖向量索引和 chunk 内容
        cacheService.evictByPrefix("rag:topk:");

        // 文档列表、系统统计可能依赖文档/向量状态
        cacheService.evict("tool:docs:list");
        cacheService.evict("tool:system:stats");

        // 已生成答案可能基于旧检索结果
        cacheService.evictByPrefix("qa:normal:");
        cacheService.evictByPrefix("qa:stream:");
        cacheService.evictByPrefix("qa:agent:");
    }

    public void indexAllChunks() {
        indexAllChunks("REBUILD_ALL");
    }

    public void indexAllChunks(String mode) {
        boolean rebuildAll = "REBUILD_ALL".equalsIgnoreCase(mode);
        boolean missingOnly = "MISSING_ONLY".equalsIgnoreCase(mode);

        var docs = documentMapper.findAllEnabled();
        int total = 0;
        int failed = 0;

        for (var doc : docs) {
            List<KbChunk> chunks;

            if (rebuildAll) {
                removeDocumentVector(doc.getId());
                chunkMapper.resetVectorStatusByDocumentId(doc.getId());
                chunks = chunkMapper.findByDocumentId(doc.getId());
            }else if (missingOnly){
                chunks = chunkMapper.findNotCompletedByDocumentId(doc.getId());
                if (chunks.isEmpty()) {
                    continue;
                }
                documentMapper.updateEmbeddingStatus(doc.getId(), "PROCESSING");
            }else {
                log.warn("未知的索引模式: {}", mode);
                return;
            }

            for (var chunk : chunks) {
                try {
                    chunkMapper.updateVectorStatus(chunk.getId(), "PROCESSING");

                    dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
                    metadata.put("documentId", doc.getId().toString());
                    metadata.put("documentName", doc.getDocumentName());
                    metadata.put("chunkId", chunk.getId().toString());
                    metadata.put("chunkIndex", chunk.getChunkIndex().toString());
                    metadata.put("category", doc.getCategory() == null ? "" : doc.getCategory());
                    metadata.put("sectionTitle", chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle());
                    metadata.put("titlePath", chunk.getTitlePath() == null ? "" : chunk.getTitlePath());
                    metadata.put("version", doc.getVersion() == null ? "1" : doc.getVersion().toString());
                    TextSegment segment = TextSegment.from(chunk.getChunkText(), metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    embeddingStore.add(embedding, segment);

                    chunkMapper.updateVectorStatus(chunk.getId(), "COMPLETED");
                    total++;
                } catch (Exception e) {
                    failed++;
                    chunkMapper.updateVectorStatus(chunk.getId(), "FAILED");
                    log.warn("切片向量化失败 [doc={}, chunk={}]: {}", doc.getDocumentName(), chunk.getChunkIndex(), e.getMessage());
                }
            }
            documentMapper.updateEmbeddingStatus(doc.getId(), failed>0?"FAILED":"COMPLETED");
        }
        evictKnowledgeCaches();
        log.info("知识库向量索引完成: 成功={}, 失败={}", total, failed);
    }

    private void removeDocumentVector(Long documentId){
        try{
            embeddingStore.removeAll(
                    MetadataFilterBuilder.metadataKey("documentId")
                            .isEqualTo(documentId.toString())
            );
            log.info("已清理文档 {} 的旧向量索引", documentId);
        }catch (Exception e){
            log.warn("清理文档 {} 的旧向量索引失败: {}", documentId, e.getMessage());
        }
    }

    public void reindexDocument(Long documentId) {
        var doc = documentMapper.findById(documentId);
        if (doc == null || !Boolean.TRUE.equals(doc.getEnabled())) return;

        removeDocumentVector(documentId);
        chunkMapper.resetVectorStatusByDocumentId(documentId);

        var chunks = chunkMapper.findByDocumentId(documentId);
        int failed = 0;

        for (var chunk : chunks) {
            try {
                chunkMapper.updateVectorStatus(chunk.getId(), "PROCESSING");

                dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
                metadata.put("documentId", doc.getId().toString());
                metadata.put("documentName", doc.getDocumentName());
                metadata.put("chunkId", chunk.getId().toString());
                metadata.put("chunkIndex", chunk.getChunkIndex().toString());
                metadata.put("category", doc.getCategory() == null ? "" : doc.getCategory());
                metadata.put("sectionTitle", chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle());
                metadata.put("titlePath", chunk.getTitlePath() == null ? "" : chunk.getTitlePath());
                metadata.put("version", doc.getVersion() == null ? "1" : doc.getVersion().toString());

                TextSegment segment = TextSegment.from(chunk.getChunkText(), metadata);
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);

                chunkMapper.updateVectorStatus(chunk.getId(), "COMPLETED");
            } catch (Exception e) {
                failed++;
                chunkMapper.updateVectorStatus(chunk.getId(), "FAILED");
            }
        }
        documentMapper.updateEmbeddingStatus(documentId, failed>0?"FAILED":"COMPLETED");

        evictKnowledgeCaches();
        log.info("重新索引文档 {}，切片数={}，失败={})", documentId, chunks.size(), failed);
    }

    public String chat(Long conversationId, String userQuestion) {
        String traceId = aiCallLogService.newTraceId();
        AiConversation conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        String filteredQ = sensitiveWordService.filter(userQuestion);

        String cacheKey = "qa:normal:" + conversationId + ":" + filteredQ;
        String cached = cacheService.getIfPresent(cacheKey,new TypeReference<String>() {});
        if (cached != null) {
            log.info("[traceId={}] 命中热门问题缓存", traceId);
            AiMessage userMsg = new AiMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole("USER");
            userMsg.setContent(filteredQ);
            messageMapper.insert(userMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, userMsg);
            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(conversationId);
            aiMsg.setRole("ASSISTANT");
            aiMsg.setContent(cached);
            aiMsg.setResponseMs(0);
            messageMapper.insert(aiMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
            shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);
            return cached;
        }

        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("USER");
        userMsg.setContent(filteredQ);
        messageMapper.insert(userMsg);
        shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, userMsg);

        String memoryText = shortTermMemoryService.getMemoryText(conversation.getUserId(), conversationId);
        String retrievalQuestion = rewriteQuestionForRetrieval(filteredQ,memoryText);

        RetrievalResult rr = retrieveContext(retrievalQuestion, traceId, conversationId);

        if (isKnowledgeMiss(rr)) {
            String aiAnswer = buildKnowledgeMissAnswer(filteredQ);
            cacheService.putValue(cacheKey, aiAnswer, Duration.ofMinutes(30));

            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(conversationId);
            aiMsg.setRole("ASSISTANT");
            aiMsg.setContent(aiAnswer);
            aiMsg.setRetrievalScore(rr.topScore());
            aiMsg.setRetrievalHitCount(rr.hitCount());
            messageMapper.insert(aiMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
            shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);
            return aiAnswer;
        }

        String prompt = buildPrompt(filteredQ, rr.context(), memoryText);

        String aiAnswer;
        long llmStart = System.currentTimeMillis();
        try {
            aiAnswer = chatModel.generate(prompt);
            int llmMs = (int)(System.currentTimeMillis() - llmStart);
            aiCallLogService.logStep(traceId, conversationId, "LLM_CALL", prompt.length(), aiAnswer.length(), llmMs, true, null);
        } catch (Exception e) {
            int llmMs = (int)(System.currentTimeMillis() - llmStart);
            aiCallLogService.logStep(traceId, conversationId, "LLM_CALL", prompt.length(), 0, llmMs, false, e.getMessage());
            log.error("调用 AI 模型失败", e);
            aiAnswer = "AI 模型调用失败：" + e.getMessage() + "。请检查 API Key 和网络配置。";
        }

        String finalAnswer = sensitiveWordService.filter(aiAnswer);
        cacheService.putValue(cacheKey, aiAnswer, Duration.ofMinutes(30));
        aiAnswer = finalAnswer;

        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent(aiAnswer);
        aiMsg.setReferenceContent(rr.context().isEmpty() ? null : rr.context());
        aiMsg.setRetrievalScore(rr.topScore());
        aiMsg.setRetrievalHitCount(rr.hitCount());
        messageMapper.insert(aiMsg);
        shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
        shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);

        return aiAnswer;
    }

    public SseEmitter chatStream(Long conversationId, String userQuestion, String modelKey, Long documentId) {
        String traceId = aiCallLogService.newTraceId();
        SseEmitter emitter = new SseEmitter(120_000L);

        AiConversation conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("会话不存在"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        String filteredQ = sensitiveWordService.filter(userQuestion);

        String cacheKey = "qs:stream:" + conversationId + ":" + filteredQ
                + (documentId != null ?  "##doc:" + documentId : "");
        String cached = cacheService.getIfPresent(cacheKey,new TypeReference<String>() {});
        if (cached != null) {
            log.info("[traceId={}] 流式命中热门问题缓存", traceId);
            AiMessage userMsg = new AiMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole("USER");
            userMsg.setContent(filteredQ);
            messageMapper.insert(userMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, userMsg);
            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(conversationId);
            aiMsg.setRole("ASSISTANT");
            aiMsg.setContent(cached);
            aiMsg.setResponseMs(0);
            messageMapper.insert(aiMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
            shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);
            try {
                emitter.send(SseEmitter.event().name("token").data(cached));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("USER");
        userMsg.setContent(filteredQ);
        messageMapper.insert(userMsg);
        shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, userMsg);

        String memoryText = shortTermMemoryService.getMemoryText(conversation.getUserId(), conversationId);
        String retrievalQuestion = rewriteQuestionForRetrieval(filteredQ,memoryText);

        RetrievalResult rr = retrieveContext(retrievalQuestion, traceId, conversationId, documentId);

        if (isKnowledgeMiss(rr)) {
            String answer = buildKnowledgeMissAnswer(filteredQ);
            cacheService.putValue(cacheKey, answer, Duration.ofMinutes(30));

            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(conversationId);
            aiMsg.setRole("ASSISTANT");
            aiMsg.setContent(answer);
            aiMsg.setRetrievalScore(rr.topScore());
            aiMsg.setRetrievalHitCount(rr.hitCount());
            messageMapper.insert(aiMsg);
            shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
            shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);

            try {
                emitter.send(SseEmitter.event().name("token").data(answer));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        String prompt = buildPrompt(filteredQ, rr.context(), memoryText);

        StringBuilder fullAnswer = new StringBuilder();
        long startTime = System.currentTimeMillis();

        StreamingChatLanguageModel selectedModel = streamingChatModel;
        com.admin.entity.AiModelConfig mc = resolveModel(modelKey);
        String usedModelKey = mc != null ? mc.getModelKey() : "default";
        if (mc != null) {
            try { selectedModel = buildStreamingModel(mc); } catch (Exception e) { log.warn("动态模型构建失败，使用默认模型: {}", e.getMessage()); }
        }

        selectedModel.generate(prompt, new StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
            @Override
            public void onNext(String token) {
                fullAnswer.append(token);
                try {
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (Exception e) {
                    log.warn("SSE send failed: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                int elapsed = (int)(System.currentTimeMillis() - startTime);
                String answer = sensitiveWordService.filter(fullAnswer.toString());
                cacheService.putValue(cacheKey, answer, Duration.ofMinutes(30));

                AiMessage aiMsg = new AiMessage();
                aiMsg.setConversationId(conversationId);
                aiMsg.setRole("ASSISTANT");
                aiMsg.setContent(answer);
                aiMsg.setReferenceContent(rr.context().isEmpty() ? null : rr.context());
                aiMsg.setResponseMs(elapsed);
                aiMsg.setRetrievalScore(rr.topScore());
                aiMsg.setRetrievalHitCount(rr.hitCount());
                messageMapper.insert(aiMsg);
                shortTermMemoryService.appendMessage(conversation.getUserId(), conversationId, aiMsg);
                shortTermMemoryService.summarizeIfNeeded(conversation.getUserId(), conversationId);

                aiCallLogService.log(traceId, conversationId, aiMsg.getId(), "LLM_CALL", usedModelKey,
                        prompt.length(), answer.length(), elapsed, true, null, null);

                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("SSE complete failed: {}", e.getMessage());
                }
            }

            @Override
            public void onError(Throwable error) {
                int elapsed = (int)(System.currentTimeMillis() - startTime);
                aiCallLogService.log(traceId, conversationId, null, "LLM_CALL", usedModelKey,
                        prompt.length(), 0, elapsed, false, error.getMessage(), null);
                log.error("流式生成失败", error);
                try {
                    emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    public void updateFeedback(Long messageId, String feedback) {
        messageMapper.updateFeedback(messageId, feedback);
    }

    private RetrievalResult retrieveContext(String question, String traceId, Long conversationId) {
        return retrieveContext(question, traceId, conversationId, null);
    }

    private RetrievalResult retrieveContext(String question, String traceId, Long conversationId, Long documentId) {
        long t0 = System.currentTimeMillis();
        try {
            long searchStart = System.currentTimeMillis();
            List<KnowledgeSearchHit> hits = knowledgeRetrievalService.hybridSearch(question, documentId, 5);
            int searchMs = (int)(System.currentTimeMillis() - searchStart);

            double topScore = hits.isEmpty() || hits.get(0).getFinalScore() == null ? 0 : hits.get(0).getFinalScore();
            int hitCount = hits.size();
            String context = hits.stream()
                    .map(hit -> String.format(
                            "[source:%s | score:%.2f | document:%s | category:%s | version:%s | section:%s | chunk:%s]\n%s",
                            hit.getSource(),
                            hit.getFinalScore() == null ? 0 : hit.getFinalScore(),
                            hit.getDocumentName() == null ? "" : hit.getDocumentName(),
                            hit.getCategory() == null ? "" : hit.getCategory(),
                            hit.getVersion() == null ? "" : hit.getVersion(),
                            hit.getSectionTitle() == null ? "" : hit.getSectionTitle(),
                            hit.getChunkIndex() == null ? "" : hit.getChunkIndex(),
                            hit.getText()))
                    .collect(Collectors.joining("\n---\n"));

            aiCallLogService.logStep(traceId, conversationId, "RETRIEVAL", question.length(), context.length(), searchMs, true, null);

            return new RetrievalResult(context, topScore, hitCount);
        } catch (Exception e) {
            int elapsed = (int)(System.currentTimeMillis() - t0);
            aiCallLogService.logStep(traceId, conversationId, "RETRIEVAL", question.length(), 0, elapsed, false, e.getMessage());
            log.warn("知识库检索失败: {}", e.getMessage());
            return new RetrievalResult("", 0, 0);
        }
    }

    record RetrievalResult(String context, double topScore, int hitCount) {}

    private boolean isKnowledgeMiss(RetrievalResult rr) {
        return rr == null || rr.hitCount() <= 0 || rr.context() == null || rr.context().isBlank();
    }

    private String buildKnowledgeMissAnswer(String question) {
        return "当前知识库中未检索到与“" + question + "”直接相关的内容，无法基于现有资料给出准确答复。\n\n"
                + "建议：\n"
                + "1. 补充相关知识库文档后再提问\n"
                + "2. 改问与当前系统内制度、流程、FAQ 更相关的问题\n"
                + "3. 如果你愿意，我可以先帮你整理这个问题需要哪些资料";
    }

    private String getPromptContent(String key, String fallback) {
        String content = cacheService.get(
                "config:prompt:" + key,
                new TypeReference<String>() {},
                Duration.ofMinutes(10),
                Duration.ofMinutes(1),
                () -> {
                    var config = promptConfigMapper.findByKey(key);
                    return config != null && config.getEnabled() ? config.getContent() :null;
                }
        );
        return content == null ? fallback : content;
    }

    private String buildPrompt(String question, String context, String memory) {
        StringBuilder sb = new StringBuilder();

        String systemPrompt = getPromptContent("SYSTEM_PROMPT",
                "你是「AI 知识工单平台」的智能助手，请直接、专业地回答用户问题。");
        sb.append(systemPrompt).append("\n");

        String formatPrompt = getPromptContent("FORMAT_PROMPT",
                "请使用 Markdown 格式输出回答。");
        sb.append(formatPrompt).append("\n\n");

        if (!context.isEmpty()) {
            String contextTpl = getPromptContent("CONTEXT_TEMPLATE", "【知识库参考内容】\n{context}");
            sb.append(contextTpl.replace("{context}", context)).append("\n\n");
        } else {
            String noHitPrompt = getPromptContent("NO_HIT_PROMPT",
                    "如果知识库没有命中相关内容，请明确说明当前知识库未检索到相关内容，不要编造公司内部制度、流程、规则或数据。");
            sb.append(noHitPrompt).append("\n\n");
        }

        if (!memory.isEmpty()) {
            String historyTpl = getPromptContent("HISTORY_TEMPLATE", "【对话历史】\n{history}");
            sb.append(historyTpl.replace("{history}", memory)).append("\n\n");
        }

        String questionTpl = getPromptContent("QUESTION_TEMPLATE", "【用户问题】\n{question}\n\n请用中文回答：");
        sb.append(questionTpl.replace("{question}", question));

        return sb.toString();
    }

    public RetrievalTestResponse retrievalTest(RetrievalTestRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()){
            throw new RuntimeException("问题不能为空");
        }

        int topK = request.getTopK() == null ? 5 : request.getTopK();
        String originalQuestion = request.getQuestion().trim();
        String retrievalQuestion = originalQuestion;
        boolean rewriteUsed = false;

        if (Boolean.TRUE.equals(request.getRewrite()) && request.getConversationId() != null){
            AiConversation conversation = conversationMapper.findById(request.getConversationId());
            if (conversation != null) {
                String memoryText = shortTermMemoryService.getMemoryText(conversation.getUserId(), request.getConversationId());
                retrievalQuestion = rewriteQuestionForRetrieval(originalQuestion, memoryText);
                rewriteUsed = !retrievalQuestion.equals(originalQuestion);
            }
        }

        boolean expandNeighbors = request.getExpandNeighbors() == null || request.getExpandNeighbors();
        boolean useCache = request.getUseCache() == null || request.getUseCache();

        List<RetrievalTestResult> response = knowledgeRetrievalService
                .searchForEvaluation(
                        retrievalQuestion,
                        request.getDocumentId(),
                        topK,
                        request.getRetrievalMode(),
                        expandNeighbors,
                        useCache
                )
                .stream()
                .map(hit -> new RetrievalTestResult(
                        hit.getDocumentId(),
                        hit.getDocumentName(),
                        hit.getChunkId(),
                        hit.getChunkIndex(),
                        hit.getFinalScore(),
                        hit.getVectorScore(),
                        hit.getBm25Score(),
                        hit.getSource(),
                        hit.getText()
                ))
                .toList();
        return new RetrievalTestResponse(
                originalQuestion,
                retrievalQuestion,
                rewriteUsed,
                response
        );
    }
}
