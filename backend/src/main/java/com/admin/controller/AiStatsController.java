package com.admin.controller;

import com.admin.dto.Result;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.AiMessageMapper;
import com.admin.mapper.KbDocumentMapper;
import com.admin.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/stats")
@RequiredArgsConstructor
public class AiStatsController {

    private final AiMessageMapper messageMapper;
    private final AiConversationMapper conversationMapper;
    private final KbDocumentMapper documentMapper;
    private final TicketMapper ticketMapper;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", messageMapper.countByFeedback("LIKE"));
        data.put("dislikeCount", messageMapper.countByFeedback("DISLIKE"));
        data.put("totalReplies", messageMapper.countAssistantMessages());
        data.put("avgResponseMs", messageMapper.avgResponseMs());
        data.put("totalTokenUsage", messageMapper.totalTokenUsage());
        data.put("conversationCount", conversationMapper.countAll());
        data.put("documentCount", documentMapper.countByCondition(null, null, null));
        data.put("ticketCount", ticketMapper.countByCondition(null, null, null, null));
        return Result.success(data);
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "14") int days) {
        String since = LocalDate.now().minusDays(days).toString();
        Map<String, Object> data = new HashMap<>();
        data.put("dailyMessages", messageMapper.dailyMessageCount(since));
        data.put("dailyConversations", messageMapper.dailyConversationCount(since));
        return Result.success(data);
    }
}
