package com.admin.service;

import com.admin.dto.KnowledgeSearchHit;
import com.admin.entity.KbDocument;
import com.admin.entity.Ticket;
import com.admin.mapper.AiConversationMapper;
import com.admin.mapper.KbDocumentMapper;
import com.admin.mapper.TicketMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final TicketMapper ticketMapper;
    private final KbDocumentMapper documentMapper;
    private final AiConversationMapper conversationMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final TwoLevelCacheService cacheService;

    @Tool("根据工单编号查询工单详情，返回工单标题、状态、优先级、客户名称和内容摘要")
    public String queryTicketByNo(String ticketNo) {
        log.info("[Tool] queryTicketByNo: {}", ticketNo);
        List<Ticket> all = ticketMapper.findAll();
        Ticket ticket = all.stream()
                .filter(t -> ticketNo.equals(t.getTicketNo()))
                .findFirst().orElse(null);
        if (ticket == null) return "未找到工单: " + ticketNo;
        return String.format("工单[%s] 标题:%s | 状态:%s | 优先级:%s | 客户:%s | 内容:%s",
                ticket.getTicketNo(), ticket.getTitle(), ticket.getTicketStatus(),
                ticket.getPriority(), ticket.getCustomerName(),
                truncate(ticket.getContent(), 200));
    }

    @Tool("根据关键词搜索工单列表，可按标题、客户名称、内容、工单编号搜索，返回最多5条匹配的工单摘要信息")
    public String searchTickets(String keyword) {
        log.info("[Tool] searchTickets: {}", keyword);
        List<Ticket> tickets = ticketMapper.searchByKeyword(keyword, 5);
        if (tickets.isEmpty()) return "未找到包含关键词[" + keyword + "]的工单";
        return tickets.stream()
                .map(t -> String.format("工单[%s] 标题:%s | 客户:%s | 状态:%s | 优先级:%s | 内容:%s",
                        t.getTicketNo(), t.getTitle(), t.getCustomerName(),
                        t.getTicketStatus(), t.getPriority(),
                        truncate(t.getContent(), 200)))
                .collect(Collectors.joining("\n"));
    }

    @Tool("查询知识库文档列表，返回文档名称、分类和状态信息")
    public String listKnowledgeDocuments() {
        log.info("[Tool] listKnowledgeDocuments");
        return cacheService.get(
                "tool:docs:list",
                new TypeReference<String>() {},
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                this::loadKnowledgeDocuments
        );
    }

    private  String loadKnowledgeDocuments() {
        List<KbDocument> docs = documentMapper.findAllEnabled();
        if (docs.isEmpty()) return "知识库暂无启用的文档";
        return docs.stream()
                .map(d -> String.format("[ID:%d] %s (分类:%s, 解析:%s, 切片数:%d)",
                        d.getId(), d.getDocumentName(), d.getCategory(),
                        d.getParseStatus(), d.getChunkCount()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("根据文档ID查询知识库文档详情，返回文档名称、分类、摘要等信息")
    public String queryDocumentById(Long documentId) {
        log.info("[Tool] queryDocumentById: {}", documentId);
        KbDocument doc = documentMapper.findById(documentId);
        if (doc == null) return "未找到文档ID: " + documentId;
        return String.format("文档[%d] 名称:%s | 分类:%s | 来源:%s | 解析状态:%s | 切片数:%d | 摘要:%s",
                doc.getId(), doc.getDocumentName(), doc.getCategory(),
                doc.getSourceType(), doc.getParseStatus(), doc.getChunkCount(),
                truncate(doc.getSummary(), 200));
    }

    @Tool("查询系统统计概览，包括会话数、文档数、工单数等关键指标")
    public String querySystemStats() {
        log.info("[Tool] querySystemStats");
        return cacheService.get(
                "tool:system:stats",
                new TypeReference<String>() {},
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                this::loadQuerySystemStats
        );
    }

    private String loadQuerySystemStats() {
        long convCount = conversationMapper.countAll();
        long docCount = documentMapper.countByCondition(null, null, null);
        long ticketCount = ticketMapper.countByCondition(null, null, null, null);
        return String.format("系统概览: 会话总数=%d, 知识库文档数=%d, 工单总数=%d",
                convCount, docCount, ticketCount);
    }

    @Tool("根据用户问题在知识库中进行混合检索，返回最相关的知识片段。当用户提出需要专业知识回答的问题时使用此工具")
    public String searchKnowledge(String query) {
        log.info("[Tool] searchKnowledge: {}", query);
        List<KnowledgeSearchHit> hits = knowledgeRetrievalService.hybridSearch(query, null, 5);
        if (hits.isEmpty()) return "知识库中未找到与[" + query + "]相关的内容";
        return hits.stream()
                .map(hit -> String.format("[来源:%s | 分数:%.2f | 召回:%s]\n%s",
                        hit.getDocumentName() != null ? hit.getDocumentName() : "未知",
                        hit.getFinalScore() != null ? hit.getFinalScore() : 0,
                        hit.getSource(),
                        hit.getText()))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("在指定文档内进行混合检索，参数为文档ID和搜索问题。当用户要求在特定文档中查找信息时使用此工具")
    public String searchDocumentContent(Long documentId, String query) {
        log.info("[Tool] searchDocumentContent: docId={}, query={}", documentId, query);
        List<KnowledgeSearchHit> hits = knowledgeRetrievalService.hybridSearch(query, documentId, 5);
        if (hits.isEmpty()) return "文档ID=" + documentId + "中未找到与[" + query + "]相关的内容";
        return hits.stream()
                .map(hit -> String.format("[分数:%.2f | 召回:%s]\n%s",
                        hit.getFinalScore() != null ? hit.getFinalScore() : 0,
                        hit.getSource(),
                        hit.getText()))
                .collect(Collectors.joining("\n---\n"));
    }

    public String executeTool(String toolName, String argumentsJson) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var args = mapper.readValue(argumentsJson, java.util.Map.class);
            return switch (toolName) {
                case "queryTicketByNo" -> queryTicketByNo((String) args.get("ticketNo"));
                case "searchTickets" -> searchTickets((String) args.get("keyword"));
                case "listKnowledgeDocuments" -> listKnowledgeDocuments();
                case "queryDocumentById" -> queryDocumentById(Long.valueOf(args.get("documentId").toString()));
                case "querySystemStats" -> querySystemStats();
                case "searchKnowledge" -> searchKnowledge((String) args.get("query"));
                case "searchDocumentContent" -> searchDocumentContent(
                        Long.valueOf(args.get("documentId").toString()), (String) args.get("query"));
                default -> "未知工具: " + toolName;
            };
        } catch (Exception e) {
            log.error("[Tool] executeTool error: {}", e.getMessage(), e);
            return "工具执行异常: " + e.getMessage();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
