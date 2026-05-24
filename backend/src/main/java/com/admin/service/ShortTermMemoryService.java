package com.admin.service;

import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.AiMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortTermMemoryService {

    private static final int WINDOW_SIZE = 20;
    private static final int RECENT_MEMORY_COUNT = 10;
    private static final Duration MEMORY_TTL = Duration.ofDays(7);
    private static final int SUMMARY_TRIGGER_COUNT = 16;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ChatLanguageModel chatModel;

    public void appendMessage(Long userId, Long conversationId, AiMessage message) {
        if (message == null || message.getContent() == null || message.getContent().isBlank()) {
            return;
        }
        try {
            MemoryMessage memoryMessage = new MemoryMessage();
            memoryMessage.setMessageId(message.getId());
            memoryMessage.setRole(message.getRole());
            memoryMessage.setContent(message.getContent());

            String key = windowKey(userId, conversationId);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(memoryMessage));
            redisTemplate.opsForList().trim(key, -WINDOW_SIZE, -1);
            redisTemplate.expire(key, MEMORY_TTL);
        }catch (Exception e){
            log.error("写入短期记忆失败 conversationId={}:{}",conversationId, e.getMessage());
        }
    }

    public String getMemoryText(Long userId, Long conversationId) {
        AiConversation conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            return null;
        }

        String summary = getSummary(userId, conversationId,conversation);
        List<MemoryMessage> window = getWindow(userId, conversationId);

        if (window.isEmpty()){
            warmupFormDb(userId, conversationId);
        }

        String recentHistory = window.stream()
                .skip(Math.max(0, window.size() - RECENT_MEMORY_COUNT))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder memory = new StringBuilder();

        if (summary != null && !summary.isBlank()){
            memory.append("【历史摘要】\n")
                    .append(summary)
                    .append("\n\n");
        }

        if (!recentHistory.isBlank()){
            memory.append("【最近对话】\n")
                    .append(recentHistory);
        }
        return memory.toString();
    }

    public void summarizeIfNeeded(Long userId, Long conversationId){
        String lockKey = summaryLockKey(userId, conversationId);
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(locked)){
            return;
        }

        try{
            AiConversation conversation = conversationMapper.findById(conversationId);
            if (conversation == null){
                return;
            }

            Long summarizedMessageSeq = conversation.getSummaryMessageSeq() == null
                    ? 0L
                    : conversation.getSummaryMessageSeq();

            List<AiMessage> newMessages = messageMapper.findAfterId(conversationId, summarizedMessageSeq);

            if (newMessages.size() < SUMMARY_TRIGGER_COUNT){
                return;
            }

            int keepRecent = Math.min(RECENT_MEMORY_COUNT, newMessages.size());
            List<AiMessage> messagesToSummarize = newMessages.subList(0,newMessages.size() - keepRecent);

            if (messagesToSummarize.isEmpty()){
                return;
            }

            String conversationText = messagesToSummarize.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));

            String oldSummary = conversation.getSummary() == null ? "" : conversation.getSummary();

            String prompt = """
                    请更新下面的会话摘要，用于后续多轮问答记忆。
                    
                    要求：
                    1. 保留用户目标、已确认事实、关键编号、重要结论、待办事项。
                    2. 删除寒暄、重复表达和无关内容。
                    3. 不要编造原对话中没有的信息。
                    4. 使用结构化中文输出。
    
                    【已有摘要】
                    %s
    
                    【新增对话】
                    %s
                    """.formatted(oldSummary, conversationText);

            String newSummary = chatModel.generate(prompt);

            Long newSummaryMessageSeq = messagesToSummarize.get(messagesToSummarize.size() - 1).getId();

            conversation.setSummary(newSummary);
            conversation.setSummaryMessageSeq(newSummaryMessageSeq);
            conversationMapper.updateSummary(conversation);

            redisTemplate.opsForValue().set(
                    summaryKey(userId, conversationId),
                    newSummary,
                    MEMORY_TTL
            );

            log.info("短期记忆摘要已更新 conversationId={}, messageId={}",
                    conversationId, newSummaryMessageSeq);
        }catch (Exception e){
            log.warn("短期记忆摘要失败 conversationId={}: {}", conversationId, e.getMessage());
        }finally {
            redisTemplate.delete(lockKey);
        }
    }

    public void clear(Long userId, Long conversationId) {
        redisTemplate.delete(windowKey(userId, conversationId));
        redisTemplate.delete(summaryKey(userId, conversationId));
        redisTemplate.delete(summaryLockKey(userId, conversationId));
    }

    private String summaryLockKey(Long userId, Long conversationId) {
        return "system:u" + safeUserId(userId) + ":c" + conversationId + ":summary_lock";
    }

    private void warmupFormDb(Long userId, Long conversationId) {
        List<AiMessage> messages = messageMapper.findByConversationId(conversationId);
        if (messages == null || messages.isEmpty()){
            return;
        }

        List<AiMessage> recent = messages.size() > WINDOW_SIZE
                ? messages.subList(messages.size() - WINDOW_SIZE, messages.size())
                : messages;

        String key = windowKey(userId, conversationId);
        redisTemplate.delete(key);

        for (AiMessage message : recent){
            appendMessage(userId, conversationId, message);
        }
    }

    private List<MemoryMessage> getWindow(Long userId, Long conversationId) {
        try {
            String key = windowKey(userId, conversationId);
            List<String> values = redisTemplate.opsForList().range(key, 0, -1);

            if (values == null || values.isEmpty()){
                return Collections.emptyList();
            }

            return values.stream()
                    .map(json ->{
                        try {
                            return objectMapper.readValue(json, MemoryMessage.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(m -> m != null)
                    .toList();
        }catch (Exception e){
            log.error("获取短期记忆失败 conversationId={}:{}",conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getSummary(Long userId, Long conversationId, AiConversation conversation) {
        String key = summaryKey(userId, conversationId);
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null){
            return cached;
        }
        String summary = conversation.getSummary();
        if (summary != null && !summary.isBlank()) {
            redisTemplate.opsForValue().set(key, summary, MEMORY_TTL);
        }
        return summary;
    }

    private String summaryKey(Long userId, Long conversationId) {
        return "system:u:" + safeUserId(userId) + ":c:" + conversationId + ":summary";
    }

    private String windowKey(Long userId, Long conversationId) {
        return "system:u:" + safeUserId(userId) + ":c:" + conversationId + ":window";
    }

    private Long safeUserId(Long userId) {
        return userId == null ? 0L : userId;
    }

    @Data
    public static class MemoryMessage{
        private Long messageId;
        private String role;
        private String content;
    }
}
