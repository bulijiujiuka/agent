package com.admin.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface AiAssistant {

    String chat(@MemoryId Object memoryId, @UserMessage String userMessage);

    TokenStream chatStream(@MemoryId Object memoryId, @UserMessage String userMessage);
}
