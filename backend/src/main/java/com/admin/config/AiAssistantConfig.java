package com.admin.config;

import com.admin.service.AiAssistant;
import com.admin.service.AiToolService;
import com.admin.service.PersistentChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiAssistantConfig {

    @Bean
    public AiAssistant aiAssistant(ChatLanguageModel chatLanguageModel,
                                   StreamingChatLanguageModel streamingChatLanguageModel,
                                   AiToolService aiToolService,
                                   PersistentChatMemoryStore chatMemoryStore) {
        return AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .tools(aiToolService)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .chatMemoryStore(chatMemoryStore)
                                .build())
                .build();
    }
}
