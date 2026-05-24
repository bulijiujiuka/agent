package com.admin.service;

import com.admin.dto.PageResult;
import com.admin.dto.TicketQueryRequest;
import com.admin.entity.Ticket;
import com.admin.exception.BusinessException;
import com.admin.mapper.TicketMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final ChatLanguageModel chatModel;
    private final AiToolService aiToolService;
    private final SensitiveWordService sensitiveWordService;
    private final AtomicLong ticketSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    public List<Ticket> getAll() {
        return ticketMapper.findAll();
    }

    public Ticket getById(Long id) {
        return ticketMapper.findById(id);
    }

    public PageResult<Ticket> getPage(TicketQueryRequest query) {
        long total = ticketMapper.countByCondition(
                query.getTitle(), query.getCategory(),
                query.getPriority(), query.getTicketStatus());
        List<Ticket> records = ticketMapper.findByPage(
                query.getTitle(), query.getCategory(),
                query.getPriority(), query.getTicketStatus(),
                query.getOffset(), query.getPageSize());
        return PageResult.of(records, total, query);
    }

    public Ticket create(Ticket ticket) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        ticket.setTicketNo("TK" + dateStr + String.format("%05d", ticketSeq.incrementAndGet()));
        if (ticket.getTicketStatus() == null) {
            ticket.setTicketStatus("OPEN");
        }
        if (ticket.getPriority() == null) {
            ticket.setPriority("MEDIUM");
        }
        ticketMapper.insert(ticket);
        final Long ticketId = ticket.getId();
        final String title = ticket.getTitle();
        final String content = ticket.getContent();
        java.util.concurrent.CompletableFuture.runAsync(() -> generateAiAssist(ticketId, title, content));
        return ticketMapper.findById(ticket.getId());
    }

    public void generateAiAssist(Long ticketId, String title, String content) {
        try {
            // 用工单标题+内容作为查询，检索知识库
            String kbContext = "";
            try {
                String searchQuery = title + " " + (content != null ? content.substring(0, Math.min(200, content.length())) : "");
                String kbResult = aiToolService.searchKnowledge(searchQuery);
                if (!kbResult.contains("知识库中未找到与[")) {
                    kbContext = kbResult;
                }
            } catch (Exception e) {
                log.warn("工单 {} 知识库检索失败: {}", ticketId, e.getMessage());
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个企业售后工单处理助手。请根据以下工单信息");
            if (!kbContext.isEmpty()) {
                promptBuilder.append("和知识库参考资料");
            }
            promptBuilder.append("，分别输出三部分内容：\n")
                    .append("1. 【问题摘要】：用一两句话概括工单核心问题\n")
                    .append("2. 【回复建议】：给客户的回复草稿（专业、友好）\n")
                    .append("3. 【处理建议】：给内部处理人员的操作步骤建议\n\n");
            if (!kbContext.isEmpty()) {
                promptBuilder.append("===== 知识库参考资料 =====\n").append(kbContext).append("\n\n");
            }
            promptBuilder.append("===== 工单信息 =====\n")
                    .append("工单标题：").append(title).append("\n")
                    .append("工单内容：").append(content).append("\n\n")
                    .append("请优先依据知识库参考资料作答，用中文回答，格式清晰：");

            String prompt = promptBuilder.toString();
            String aiResult = sensitiveWordService.filter(chatModel.generate(prompt));

            String aiSummary = extractSection(aiResult, "问题摘要");
            String aiReply = extractSection(aiResult, "回复建议");
            String aiSuggestion = extractSection(aiResult, "处理建议");

            Ticket update = new Ticket();
            update.setId(ticketId);
            update.setAiSummary(aiSummary.isEmpty() ? aiResult.substring(0, Math.min(500, aiResult.length())) : aiSummary);
            update.setAiReply(aiReply);
            update.setAiSuggestion(aiSuggestion);
            ticketMapper.update(update);
            log.info("工单 {} AI 辅助生成完成", ticketId);
        } catch (Exception e) {
            log.error("工单 {} AI 辅助生成失败: {}", ticketId, e.getMessage());
        }
    }

    private String extractSection(String text, String sectionName) {
        String[] markers = {"【" + sectionName + "】", sectionName + "：", sectionName + ":"};
        for (String marker : markers) {
            int start = text.indexOf(marker);
            if (start >= 0) {
                start += marker.length();
                int end = text.length();
                String[] nextMarkers = {"【", "\n\n"};
                for (String next : nextMarkers) {
                    int nextIdx = text.indexOf(next, start + 1);
                    if (nextIdx > start && nextIdx < end) {
                        end = nextIdx;
                    }
                }
                return text.substring(start, end).trim();
            }
        }
        return "";
    }

    public int update(Ticket ticket) {
        Ticket existing = ticketMapper.findById(ticket.getId());
        if (existing == null) {
            throw new BusinessException("工单不存在");
        }
        return ticketMapper.update(ticket);
    }

    public int delete(Long id) {
        return ticketMapper.deleteById(id);
    }
}
