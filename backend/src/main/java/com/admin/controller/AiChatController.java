package com.admin.controller;

import com.admin.dto.*;
import com.admin.entity.AiMessage;
import com.admin.mapper.AiMessageMapper;
import com.admin.service.AiChatService;
import com.admin.service.AiToolService;
import com.admin.service.PersistentChatMemoryStore;
import com.admin.service.TwoLevelCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiMessageMapper messageMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final AiToolService aiToolService;
    private final PersistentChatMemoryStore chatMemoryStore;
    private final com.admin.service.SensitiveWordService sensitiveWordService;
    private final TwoLevelCacheService cacheService;

    @PostMapping
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        String answer = aiChatService.chat(request.getConversationId(), request.getQuestion());
        return Result.success(new AiChatResponse(answer, null));
    }

    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChatStream(@RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Long convId = request.getConversationId();
        String question = sensitiveWordService.filter(request.getQuestion());

        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(convId);
        userMsg.setRole("USER");
        userMsg.setContent(question);
        messageMapper.insert(userMsg);

        String cacheKey = "agent:" + convId + ":" + question;
        String cached = cacheService.getIfPresent(cacheKey, new TypeReference<String>() {});
        if (cached != null) {
            log.info("[Agent] 命中缓存: {}", question);
            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(convId);
            aiMsg.setRole("ASSISTANT");
            aiMsg.setContent(cached);
            aiMsg.setResponseMs(0);
            messageMapper.insert(aiMsg);
            try {
                emitter.send(SseEmitter.event().name("token").data(cached));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        var memoryKey = new PersistentChatMemoryStore.MemoryKey(0L, convId);

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            try {
                List<ChatMessage> history = new ArrayList<>(chatMemoryStore.getMessages(memoryKey));

                // Phase 1: 同步工具调用，只收集工具结果文本
                List<ChatMessage> toolLoop = new ArrayList<>(history);
                toolLoop.add(UserMessage.from(question));
                List<ToolSpecification> toolSpecs = ToolSpecifications.toolSpecificationsFrom(aiToolService);
                StringBuilder toolContext = new StringBuilder();

                for (int i = 0; i < 3; i++) {
                    Response<dev.langchain4j.data.message.AiMessage> resp = chatLanguageModel.generate(toolLoop, toolSpecs);
                    dev.langchain4j.data.message.AiMessage aiResp = resp.content();
                    if (!aiResp.hasToolExecutionRequests()) break;
                    toolLoop.add(aiResp);
                    for (var toolReq : aiResp.toolExecutionRequests()) {
                        log.info("[Agent] 调用工具: {} args={}", toolReq.name(), toolReq.arguments());
                        String result = aiToolService.executeTool(toolReq.name(), toolReq.arguments());
                        toolLoop.add(ToolExecutionResultMessage.from(toolReq, result));
                        toolContext.append("[").append(toolReq.name()).append("]: ").append(result).append("\n");
                    }
                }

                // Phase 1.5: 如果LLM没调知识库工具或调了但未命中，强制做一次兜底检索
                if (isKnowledgeMiss(toolContext.toString())) {
                    log.info("[Agent] 工具未命中或未调用，执行强制知识库兜底检索: {}", question);
                    try {
                        String fallbackResult = aiToolService.searchKnowledge(question);
                        if (!fallbackResult.contains("知识库中未找到与[")) {
                            toolContext.append("[searchKnowledge-fallback]: ").append(fallbackResult).append("\n");
                            log.info("[Agent] 兜底检索命中知识库");
                        }
                    } catch (Exception e) {
                        log.warn("[Agent] 兜底检索异常: {}", e.getMessage());
                    }
                }

                // 从 toolContext 提取检索统计和引用内容
                int retrievalHitCount = extractHitCount(toolContext.toString());
                double retrievalScore = extractTopScore(toolContext.toString());
                String referenceContent = extractKnowledgeReferences(toolContext.toString());

                // 最终判断：兜底检索后仍未命中，返回固定答复
                if (isKnowledgeMiss(toolContext.toString())) {
                    String answer = buildKnowledgeMissAnswer(question);
                    //将回答缓存起来，下次直接返回
                    cacheService.putValue(cacheKey, answer, Duration.ofMinutes(30));

                    history.add(UserMessage.from(question));
                    history.add(dev.langchain4j.data.message.AiMessage.from(answer));
                    chatMemoryStore.updateMessages(memoryKey, history);

                    AiMessage aiMsg = new AiMessage();
                    aiMsg.setConversationId(convId);
                    aiMsg.setRole("ASSISTANT");
                    aiMsg.setContent(answer);
                    aiMsg.setResponseMs((int) (System.currentTimeMillis() - startTime));
                    aiMsg.setRetrievalHitCount(0);
                    aiMsg.setRetrievalScore(0.0);
                    messageMapper.insert(aiMsg);

                    try {
                        emitter.send(SseEmitter.event().name("token").data(answer));
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (Exception ignored) {}
                    return;
                }

                // Phase 2: 构建干净消息列表 → 真正流式生成
                List<ChatMessage> streamMsgs = new ArrayList<>(history);
                streamMsgs.add(dev.langchain4j.data.message.SystemMessage.from(
                        "你是「AI 知识工单平台」的智能助手。对于纯寒暄、帮助说明类问题，可以正常简短回答；对于制度、流程、规则、知识问答类问题，必须优先依据知识库和工具结果作答。若未检索到相关依据，请明确说明当前知识库暂无相关内容，不要编造公司内部制度、流程、规则、数据或办理方案。"));
                if (toolContext.length() > 0) {
                    streamMsgs.add(dev.langchain4j.data.message.SystemMessage.from(
                            "以下是通过工具查询获得的参考信息，请据此回答用户问题：\n" + toolContext));
                }
                streamMsgs.add(UserMessage.from(question));

                StringBuilder fullAnswer = new StringBuilder();
                CountDownLatch latch = new CountDownLatch(1);

                streamingChatLanguageModel.generate(streamMsgs,
                        new StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        fullAnswer.append(token);
                        try { emitter.send(SseEmitter.event().name("token").data(token)); } catch (Exception ignored) {}
                    }
                    @Override
                    public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                        int elapsed = (int)(System.currentTimeMillis() - startTime);
                        String answer = sensitiveWordService.filter(fullAnswer.toString());
                        //将回答缓存起来，下次直接返回
                        cacheService.putValue(cacheKey, answer, Duration.ofMinutes(30));
                        // 只保存干净的 USER + ASSISTANT 到记忆
                        history.add(UserMessage.from(question));
                        history.add(dev.langchain4j.data.message.AiMessage.from(answer));
                        chatMemoryStore.updateMessages(memoryKey, history);

                        AiMessage aiMsg = new AiMessage();
                        aiMsg.setConversationId(convId);
                        aiMsg.setRole("ASSISTANT");
                        aiMsg.setContent(answer);
                        aiMsg.setResponseMs(elapsed);
                        aiMsg.setRetrievalHitCount(retrievalHitCount);
                        aiMsg.setRetrievalScore(retrievalScore);
                        if (referenceContent != null && !referenceContent.isEmpty()) {
                            aiMsg.setReferenceContent(referenceContent);
                        }
                        messageMapper.insert(aiMsg);
                        try { emitter.send(SseEmitter.event().name("done").data("[DONE]")); emitter.complete(); } catch (Exception ignored) {}
                        latch.countDown();
                    }
                    @Override
                    public void onError(Throwable error) {
                        log.error("[Agent] streaming error", error);
                        try { emitter.send(SseEmitter.event().name("error").data(error.getMessage())); emitter.complete(); } catch (Exception ex) { emitter.completeWithError(ex); }
                        latch.countDown();
                    }
                });
                latch.await();
            } catch (Exception e) {
                log.error("[Agent] error", e);
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); emitter.complete(); } catch (Exception ex) { emitter.completeWithError(ex); }
            }
        }).start();

        return emitter;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        return aiChatService.chatStream(request.getConversationId(), request.getQuestion(), request.getModelKey(), request.getDocumentId());
    }

    @PostMapping("/index")
    public Result<AiChatService.IndexTaskStatus> indexKnowledge(
            @RequestParam(defaultValue = "REBUILD_ALL") String mode) {
        return Result.success(aiChatService.startIndexTask(mode));
    }

    @GetMapping("/index/task/{taskId}")
    public Result<AiChatService.IndexTaskStatus> getIndexTask(@PathVariable String taskId){
        return Result.success(aiChatService.getIndexTask(taskId));
    }

    @PostMapping("retrieval-test")
    public Result<RetrievalTestResponse> retrievalTest(@RequestBody RetrievalTestRequest request) {
        return Result.success(aiChatService.retrievalTest(request));
    }

    @PutMapping("/message/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String feedback = body.get("feedback");
        aiChatService.updateFeedback(id, feedback);
        return Result.success();
    }

    private boolean isKnowledgeMiss(String toolContext) {
        if (toolContext == null || toolContext.isBlank()) return true;
        return toolContext.contains("知识库中未找到与[")
                || toolContext.contains("中未找到与[")
                || toolContext.contains("未找到文档ID:");
    }

    private int extractHitCount(String toolContext) {
        if (toolContext == null || toolContext.isBlank()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = toolContext.indexOf("分数:", idx)) != -1) {
            count++;
            idx += 3;
        }
        return count;
    }

    private double extractTopScore(String toolContext) {
        if (toolContext == null || toolContext.isBlank()) return 0;
        double maxScore = 0;
        int idx = 0;
        while ((idx = toolContext.indexOf("分数:", idx)) != -1) {
            idx += 3;
            int end = toolContext.indexOf("|", idx);
            if (end < 0) end = toolContext.indexOf("]", idx);
            if (end > idx) {
                try {
                    double score = Double.parseDouble(toolContext.substring(idx, end).trim());
                    if (score > maxScore) maxScore = score;
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxScore;
    }

    private String extractKnowledgeReferences(String toolContext) {
        if (toolContext == null || toolContext.isBlank()) return null;
        StringBuilder refs = new StringBuilder();
        // 匹配 [来源:xxx | 分数:xxx] 开头的段落
        String[] lines = toolContext.split("\n");
        String currentSource = null;
        StringBuilder currentContent = new StringBuilder();
        for (String line : lines) {
            if (line.contains("[来源:") && line.contains("分数:")) {
                if (currentSource != null && currentContent.length() > 0) {
                    if (refs.length() > 0) refs.append("\n===\n");
                    refs.append(currentSource).append("\n").append(currentContent.toString().trim());
                }
                currentSource = line.trim();
                currentContent = new StringBuilder();
            } else if (currentSource != null && !line.equals("---") && !line.startsWith("[searchKnowledge")) {
                currentContent.append(line).append("\n");
            }
        }
        if (currentSource != null && currentContent.length() > 0) {
            if (refs.length() > 0) refs.append("\n===\n");
            refs.append(currentSource).append("\n").append(currentContent.toString().trim());
        }
        return refs.length() > 0 ? refs.toString() : null;
    }

    private String buildKnowledgeMissAnswer(String question) {
        return "当前知识库中未检索到与“" + question + "”直接相关的内容，无法基于现有资料给出准确答复。\n\n"
                + "建议：\n"
                + "1. 补充相关知识库文档后再提问\n"
                + "2. 改问与当前系统内制度、流程、FAQ 更相关的问题\n"
                + "3. 如果你愿意，我可以先帮你整理这个问题需要哪些资料";
    }
}
