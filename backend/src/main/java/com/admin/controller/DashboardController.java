package com.admin.controller;

import com.admin.dto.Result;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.KbDocumentMapper;
import com.admin.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final KbDocumentMapper documentMapper;
    private final TicketMapper ticketMapper;
    private final AiConversationMapper conversationMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("documentCount", documentMapper.countByCondition(null, null, null));
        data.put("ticketCount", ticketMapper.countByCondition(null, null, null, null));
        data.put("conversationCount", conversationMapper.countAll());
        return Result.success(data);
    }
}
