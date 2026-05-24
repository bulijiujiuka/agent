package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/conversation")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService conversationService;

    @GetMapping("/list")
    public Result<List<AiConversation>> list(@RequestParam Long userId) {
        return Result.success(conversationService.getByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<AiConversation> getById(@PathVariable Long id) {
        return Result.success(conversationService.getById(id));
    }

    @PostMapping
    public Result<AiConversation> create(@RequestBody AiConversation conversation) {
        return Result.success(conversationService.create(conversation));
    }

    @PutMapping
    public Result<Void> update(@RequestBody AiConversation conversation) {
        conversationService.update(conversation);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result<List<AiMessage>> getMessages(@PathVariable Long id) {
        return Result.success(conversationService.getMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Result<AiMessage> addMessage(@PathVariable Long id, @RequestBody AiMessage message) {
        message.setConversationId(id);
        return Result.success(conversationService.addMessage(message));
    }
}
