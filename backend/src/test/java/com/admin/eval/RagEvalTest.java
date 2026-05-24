package com.admin.eval;

import com.admin.dto.RetrievalTestRequest;
import com.admin.dto.RetrievalTestResponse;
import com.admin.dto.RetrievalTestResult;
import com.admin.entity.AiConversation;
import com.admin.entity.AiMessage;
import com.admin.service.AiChatService;
import com.admin.service.AiConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SpringBootTest
class RagEvalTest {

    private static final int ROUNDS = 3;
    private static final int TOP_K = 5;
    private static final String NO_HIT = "__NO_HIT__";
    private static final Map<String, String> EXPECTED_DOCUMENT_IDS = Map.of(
            "01_员工入职指南.txt", "2",
            "02_IT运维故障处理手册.txt", "3",
            "03_产品售后服务政策.txt", "4",
            "04_企业信息安全管理制度.txt", "5",
            "05_项目管理规范.txt", "6",
            "06_财务报销制度.txt", "7",
            "07_客户常见问题FAQ.txt", "8",
            "08_考勤与请假制度.txt", "9"
    );

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiConversationService conversationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runRagEvaluation() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();
        Path casesPath = resolveCasesPath(projectRoot);
        Path reportsDir = projectRoot.resolve("eval/reports");
        Files.createDirectories(reportsDir);

        List<EvalCase> cases = loadCases(casesPath);
        List<Detail> details = new ArrayList<>();

        for (int round = 1; round <= ROUNDS; round++) {
            for (EvalCase evalCase : cases) {
                details.add(evaluate(round, "VECTOR", evalCase, false));
                details.add(evaluate(round, "HYBRID", evalCase, false));
                if ("followup".equals(evalCase.questionType())) {
                    details.add(evaluate(round, "HYBRID_REWRITE", evalCase, true));
                }
            }
        }

        Map<String, Object> report = buildReport(cases, details);
        writeDetailsCsv(reportsDir.resolve("rag_eval_details.csv"), details);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(reportsDir.resolve("rag_eval_summary.json").toFile(), report);
    }

    private Detail evaluate(int round, String mode, EvalCase evalCase, boolean rewrite) {
        Long conversationId = null;
        try {
            if (rewrite) {
                conversationId = createContextConversation(evalCase);
            }

            RetrievalTestRequest request = new RetrievalTestRequest();
            request.setQuestion(evalCase.question());
            request.setTopK(TOP_K);
            request.setRetrievalMode("VECTOR".equals(mode) ? "VECTOR" : "HYBRID");
            request.setExpandNeighbors(false);
            request.setUseCache(false);
            request.setRewrite(rewrite);
            request.setConversationId(conversationId);

            long start = System.currentTimeMillis();
            RetrievalTestResponse response = aiChatService.retrievalTest(request);
            long latencyMs = System.currentTimeMillis() - start;

            List<RetrievalTestResult> ranked = response.getResults().stream()
                    .filter(result -> !"NEIGHBOR".equalsIgnoreCase(result.getSource()))
                    .limit(TOP_K)
                    .toList();

            String expectedDocumentId = expectedDocumentId(evalCase.expectedDocument());
            int rank = rankOf(ranked, evalCase.expectedDocument(), expectedDocumentId);
            boolean noHitExpected = NO_HIT.equals(evalCase.expectedDocument());
            boolean noHitSuccess = noHitExpected && ranked.isEmpty();
            boolean keywordHit = keywordHit(ranked, evalCase.expectedKeywords());

            return new Detail(
                    round,
                    evalCase.id(),
                    evalCase.questionType(),
                    mode,
                    evalCase.question(),
                    response.getRetrievalQuestion(),
                    response.getRewriteUsed(),
                    evalCase.expectedDocument(),
                    expectedDocumentId,
                    rank,
                    rank == 1,
                    rank > 0 && rank <= TOP_K,
                    rank > 0 ? 1.0 / rank : 0.0,
                    keywordHit,
                    noHitExpected,
                    noHitSuccess,
                    latencyMs,
                    ranked.isEmpty() ? "" : nullToEmpty(ranked.get(0).getDocumentId()),
                    ranked.isEmpty() ? "" : nullToEmpty(ranked.get(0).getDocumentName()),
                    ranked.isEmpty() || ranked.get(0).getScore() == null ? 0.0 : ranked.get(0).getScore(),
                    ranked.isEmpty() ? "" : nullToEmpty(ranked.get(0).getSource())
            );
        } finally {
            if (conversationId != null) {
                try {
                    conversationService.delete(conversationId);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Long createContextConversation(EvalCase evalCase) {
        AiConversation conversation = new AiConversation();
        conversation.setTitle("rag-eval-" + evalCase.id());
        conversation.setBusinessType("RAG_EVAL");
        conversation.setUserId(0L);
        conversation.setModelName("eval");
        conversation = conversationService.create(conversation);

        AiMessage user = new AiMessage();
        user.setConversationId(conversation.getId());
        user.setRole("USER");
        user.setContent(evalCase.contextQuestion());
        conversationService.addMessage(user);

        AiMessage assistant = new AiMessage();
        assistant.setConversationId(conversation.getId());
        assistant.setRole("ASSISTANT");
        assistant.setContent(evalCase.contextAnswer());
        conversationService.addMessage(assistant);

        return conversation.getId();
    }

    private String expectedDocumentId(String expectedDocument) {
        if (NO_HIT.equals(expectedDocument)) {
            return "";
        }
        return EXPECTED_DOCUMENT_IDS.getOrDefault(expectedDocument, "");
    }

    private int rankOf(List<RetrievalTestResult> ranked, String expectedDocument, String expectedDocumentId) {
        if (NO_HIT.equals(expectedDocument)) {
            return 0;
        }
        for (int i = 0; i < ranked.size(); i++) {
            String documentId = nullToEmpty(ranked.get(i).getDocumentId());
            if (!expectedDocumentId.isBlank() && expectedDocumentId.equals(documentId)) {
                return i + 1;
            }
            String documentName = nullToEmpty(ranked.get(i).getDocumentName());
            if (documentName.equals(expectedDocument) || documentName.contains(expectedDocument)) {
                return i + 1;
            }
        }
        return 0;
    }

    private boolean keywordHit(List<RetrievalTestResult> ranked, String expectedKeywords) {
        if (expectedKeywords == null || expectedKeywords.isBlank()) {
            return false;
        }
        String text = ranked.stream()
                .map(RetrievalTestResult::getText)
                .map(RagEvalTest::nullToEmpty)
                .reduce("", (left, right) -> left + "\n" + right);
        for (String keyword : expectedKeywords.split("\\|")) {
            if (!keyword.isBlank() && text.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildReport(List<EvalCase> cases, List<Detail> details) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("caseSource", resolveCasesPath(Path.of(System.getProperty("user.dir")).getParent()).toString());
        report.put("rounds", ROUNDS);
        report.put("caseCount", cases.size());
        report.put("topK", TOP_K);
        report.put("notes", "NEIGHBOR chunks are filtered. Retrieval cache is disabled. HYBRID_REWRITE is evaluated on followup cases only.");

        List<Map<String, Object>> summaries = new ArrayList<>();
        for (String mode : List.of("VECTOR", "HYBRID", "HYBRID_REWRITE")) {
            List<Detail> modeRows = details.stream()
                    .filter(row -> mode.equals(row.mode()))
                    .toList();
            if (!modeRows.isEmpty()) {
                summaries.add(summarize(mode, modeRows));
            }
        }
        report.put("summaries", summaries);

        List<Map<String, Object>> byType = new ArrayList<>();
        for (String mode : List.of("VECTOR", "HYBRID", "HYBRID_REWRITE")) {
            for (String type : List.of("direct", "synonym", "fuzzy", "followup", "boundary", "irrelevant")) {
                List<Detail> rows = details.stream()
                        .filter(row -> mode.equals(row.mode()))
                        .filter(row -> type.equals(row.questionType()))
                        .toList();
                if (!rows.isEmpty()) {
                    Map<String, Object> summary = summarize(mode, rows);
                    summary.put("questionType", type);
                    byType.add(summary);
                }
            }
        }
        report.put("byQuestionType", byType);

        report.put("misses", details.stream()
                .filter(row -> !row.noHitExpected())
                .filter(row -> !row.hitAt5())
                .map(row -> Map.of(
                        "round", row.round(),
                        "id", row.id(),
                        "mode", row.mode(),
                        "type", row.questionType(),
                        "question", row.question(),
                        "expectedDocument", row.expectedDocument(),
                        "expectedDocumentId", row.expectedDocumentId(),
                        "topDocumentId", row.topDocumentId(),
                        "topDocument", row.topDocument()
                ))
                .toList());

        return report;
    }

    private Map<String, Object> summarize(String mode, List<Detail> rows) {
        List<Detail> answerable = rows.stream()
                .filter(row -> !row.noHitExpected())
                .toList();
        List<Detail> noHitRows = rows.stream()
                .filter(Detail::noHitExpected)
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", mode);
        summary.put("samples", rows.size());
        summary.put("answerableSamples", answerable.size());
        summary.put("hitAt1", rate(answerable, Detail::hitAt1));
        summary.put("hitAt5", rate(answerable, Detail::hitAt5));
        summary.put("mrr", round(answerable.stream().mapToDouble(Detail::reciprocalRank).average().orElse(0.0)));
        summary.put("keywordHitAt5", rate(answerable, Detail::keywordHit));
        summary.put("noHitSamples", noHitRows.size());
        summary.put("noHitAccuracy", rate(noHitRows, Detail::noHitSuccess));
        summary.put("p50LatencyMs", percentile(rows.stream().map(Detail::latencyMs).toList(), 0.50));
        summary.put("p95LatencyMs", percentile(rows.stream().map(Detail::latencyMs).toList(), 0.95));
        return summary;
    }

    private static double rate(List<Detail> rows, BoolMetric metric) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        long hits = rows.stream().filter(metric::test).count();
        return round(hits * 100.0 / rows.size());
    }

    private static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<EvalCase> loadCases(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<EvalCase> cases = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> cells = parseCsvLine(lines.get(i));
            cases.add(new EvalCase(
                    cells.get(0),
                    cells.get(1),
                    cells.get(2),
                    cells.get(3),
                    cells.get(4),
                    cells.get(5),
                    cells.get(6)
            ));
        }
        return cases;
    }

    private Path resolveCasesPath(Path projectRoot) {
        String configured = System.getProperty("rag.eval.cases");
        if (configured == null || configured.isBlank()) {
            return projectRoot.resolve("eval/rag_eval_cases.csv");
        }
        Path path = Path.of(configured);
        return path.isAbsolute() ? path : projectRoot.resolve(path);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private void writeDetailsCsv(Path path, List<Detail> details) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("round,id,question_type,mode,question,retrieval_question,rewrite_used,expected_document,expected_document_id,rank,hit_at_1,hit_at_5,reciprocal_rank,keyword_hit,no_hit_expected,no_hit_success,latency_ms,top_document_id,top_document,top_score,top_source");
        for (Detail detail : details) {
            lines.add(String.join(",",
                    String.valueOf(detail.round()),
                    csv(detail.id()),
                    csv(detail.questionType()),
                    csv(detail.mode()),
                    csv(detail.question()),
                    csv(detail.retrievalQuestion()),
                    String.valueOf(detail.rewriteUsed()),
                    csv(detail.expectedDocument()),
                    csv(detail.expectedDocumentId()),
                    String.valueOf(detail.rank()),
                    String.valueOf(detail.hitAt1()),
                    String.valueOf(detail.hitAt5()),
                    String.format(Locale.ROOT, "%.4f", detail.reciprocalRank()),
                    String.valueOf(detail.keywordHit()),
                    String.valueOf(detail.noHitExpected()),
                    String.valueOf(detail.noHitSuccess()),
                    String.valueOf(detail.latencyMs()),
                    csv(detail.topDocumentId()),
                    csv(detail.topDocument()),
                    String.format(Locale.ROOT, "%.4f", detail.topScore()),
                    csv(detail.topSource())
            ));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        return "\"" + nullToEmpty(value).replace("\"", "\"\"") + "\"";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record EvalCase(String id, String questionType, String question, String expectedDocument,
                            String expectedKeywords, String contextQuestion, String contextAnswer) {
    }

    private record Detail(int round, String id, String questionType, String mode, String question,
                          String retrievalQuestion, Boolean rewriteUsed, String expectedDocument,
                          String expectedDocumentId, int rank,
                          boolean hitAt1, boolean hitAt5, double reciprocalRank, boolean keywordHit,
                          boolean noHitExpected, boolean noHitSuccess, long latencyMs, String topDocumentId,
                          String topDocument, double topScore, String topSource) {
    }

    @FunctionalInterface
    private interface BoolMetric {
        boolean test(Detail detail);
    }
}
