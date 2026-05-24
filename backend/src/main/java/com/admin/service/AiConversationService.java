package com.admin.service;

import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.exception.BusinessException;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.AiMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ShortTermMemoryService shortTermMemoryService;

    public List<AiConversation> getByUserId(Long userId) {
        return conversationMapper.findByUserId(userId);
    }

    public AiConversation getById(Long id) {
        return conversationMapper.findById(id);
    }

    public AiConversation create(AiConversation conversation) {
        conversation.setConversationNo(UUID.randomUUID().toString().replace("-", ""));
        conversation.setStatus("ACTIVE");
        conversationMapper.insert(conversation);
        return conversation;
    }

    public int update(AiConversation conversation) {
        return conversationMapper.update(conversation);
    }

    @Transactional
    public void delete(Long id) {
        AiConversation conv = conversationMapper.findById(id);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        messageMapper.deleteByConversationId(id);
        conversationMapper.deleteById(id);
        shortTermMemoryService.clear(conv.getUserId(), id);
    }

    public List<AiMessage> getMessages(Long conversationId) {
        return messageMapper.findByConversationId(conversationId);
    }

    public AiMessage addMessage(AiMessage message) {
        messageMapper.insert(message);
        return message;
    }
}
