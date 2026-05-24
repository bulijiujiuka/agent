package com.admin.service;

import com.admin.entity.AiChatMemory;
import com.admin.mapper.AiChatMemoryMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final AiChatMemoryMapper chatMemoryMapper;

    private static final int MAX_MESSAGES = 20;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        MemoryKey key = parseKey(memoryId);
        List<AiChatMemory> records = chatMemoryMapper.findByConversationId(key.conversationId());
        List<ChatMessage> messages = new ArrayList<>();
        for (AiChatMemory record : records) {
            switch (record.getRole()) {
                case "SYSTEM" -> messages.add(SystemMessage.from(record.getContent()));
                case "USER" -> messages.add(UserMessage.from(record.getContent()));
                case "ASSISTANT" -> messages.add(AiMessage.from(record.getContent()));
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        MemoryKey key = parseKey(memoryId);
        chatMemoryMapper.deleteByConversationId(key.conversationId());
        int seq = 0;
        List<ChatMessage> trimmed = messages.size() > MAX_MESSAGES
                ? messages.subList(messages.size() - MAX_MESSAGES, messages.size())
                : messages;
        for (ChatMessage msg : trimmed) {
            if (msg instanceof ToolExecutionResultMessage) continue;
            if (msg instanceof AiMessage am && am.text() == null) continue;
            AiChatMemory record = new AiChatMemory();
            record.setUserId(key.userId());
            record.setConversationId(key.conversationId());
            record.setRole(resolveRole(msg));
            record.setContent(resolveContent(msg));
            record.setSeq(seq++);
            chatMemoryMapper.insert(record);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        MemoryKey key = parseKey(memoryId);
        chatMemoryMapper.deleteByConversationId(key.conversationId());
    }

    private String resolveRole(ChatMessage msg) {
        if (msg instanceof SystemMessage) return "SYSTEM";
        if (msg instanceof UserMessage) return "USER";
        if (msg instanceof AiMessage) return "ASSISTANT";
        return "USER";
    }

    private String resolveContent(ChatMessage msg) {
        if (msg instanceof SystemMessage sm) return sm.text();
        if (msg instanceof UserMessage um) return um.singleText();
        if (msg instanceof AiMessage am) return am.text();
        return msg.toString();
    }

    private MemoryKey parseKey(Object memoryId) {
        if (memoryId instanceof MemoryKey mk) return mk;
        return new MemoryKey(0L, Long.parseLong(memoryId.toString()));
    }

    public record MemoryKey(Long userId, Long conversationId) {
        @Override
        public String toString() {
            return userId + ":" + conversationId;
        }
    }
}
