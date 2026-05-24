package com.admin.controller;

import com.admin.dto.PageResult;
import com.admin.dto.Result;
import com.admin.entity.AiCallLog;
import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.mapper.AiCallLogMapper;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.AiMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/audit")
@RequiredArgsConstructor
public class AiAuditController {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiCallLogMapper callLogMapper;

    @GetMapping("/conversations")
    public Result<PageResult<AiConversation>> conversations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        long total = conversationMapper.countByCondition(userId, status);
        List<AiConversation> list = conversationMapper.findByPage(userId, status, (page - 1) * size, size);
        return Result.success(new PageResult<>(list, total, page, size));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable Long id) {
        return Result.success(messageMapper.findByConversationId(id));
    }

    @GetMapping("/call-logs")
    public Result<PageResult<AiCallLog>> callLogs(
            @RequestParam(required = false) String stepName,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        long total = callLogMapper.countByCondition(stepName, success);
        List<AiCallLog> list = callLogMapper.findByPage(stepName, success, (page - 1) * size, size);
        return Result.success(new PageResult<>(list, total, page, size));
    }

    @GetMapping("/call-logs/trace/{traceId}")
    public Result<List<AiCallLog>> traceDetail(@PathVariable String traceId) {
        return Result.success(callLogMapper.findByTraceId(traceId));
    }

    @GetMapping("/call-logs/stats")
    public Result<List<Map<String, Object>>> callLogStats(@RequestParam(defaultValue = "7") int days) {
        String since = java.time.LocalDate.now().minusDays(days).toString();
        return Result.success(callLogMapper.statsByStep(since));
    }

    @GetMapping("/retrieval-eval")
    public Result<Map<String, Object>> retrievalEval() {
        Map<String, Object> data = new HashMap<>();
        data.put("avgRetrievalScore", messageMapper.avgRetrievalScore());
        data.put("avgRetrievalHitCount", messageMapper.avgRetrievalHitCount());
        data.put("countWithHits", messageMapper.countWithHits());
        data.put("countWithoutHits", messageMapper.countWithoutHits());
        long totalAssistant = messageMapper.countAssistantMessages();
        long withHits = messageMapper.countWithHits();
        data.put("hitRate", totalAssistant > 0 ? Math.round(withHits * 10000.0 / totalAssistant) / 100.0 : 0);
        data.put("avgResponseMs", messageMapper.avgResponseMs());
        data.put("likeCount", messageMapper.countByFeedback("LIKE"));
        data.put("dislikeCount", messageMapper.countByFeedback("DISLIKE"));
        long likeCount = messageMapper.countByFeedback("LIKE");
        long dislikeCount = messageMapper.countByFeedback("DISLIKE");
        long feedbackTotal = likeCount + dislikeCount;
        data.put("satisfactionRate", feedbackTotal > 0 ? Math.round(likeCount * 10000.0 / feedbackTotal) / 100.0 : 0);
        return Result.success(data);
    }
}
