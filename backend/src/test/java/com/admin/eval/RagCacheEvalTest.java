package com.admin.eval;

import com.admin.dto.KnowledgeSearchHit;
import com.admin.service.KnowledgeRetrievalService;
import com.admin.service.TwoLevelCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SpringBootTest
class RagCacheEvalTest {

    private static final int ROUNDS = 3;
    private static final int TOP_K = 5;
    private static final String NO_HIT = "__NO_HIT__";

    @Autowired
    private KnowledgeRetrievalService retrievalService;

    @Autowired
    private TwoLevelCacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runRagCacheEvaluation() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();
        Path casesPath = resolveCasesPath(projectRoot);
        Path reportsDir = projectRoot.resolve("eval/reports");
        Files.createDirectories(reportsDir);

        List<EvalCase> cases = loadCases(casesPath).stream()
                .filter(evalCase -> !NO_HIT.equals(evalCase.expectedDocument()))
                .toList();

        List<Detail> details = new ArrayList<>();
        for (int round = 1; round <= ROUNDS; round++) {
            for (EvalCase evalCase : cases) {
                evictRetrievalCache();

                TimedResult cold = timedSearch(evalCase.question());
                TimedResult warm = timedSearch(evalCase.question());

                details.add(new Detail(
                        round,
                        evalCase.id(),
                        evalCase.questionType(),
                        evalCase.question(),
                        cold.latencyMs(),
                        warm.latencyMs(),
                        sameTopResults(cold.hits(), warm.hits())
                ));
            }
        }

        Map<String, Object> report = buildReport(casesPath, cases.size(), details);
        writeDetailsCsv(reportsDir.resolve("rag_cache_eval_details.csv"), details);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(reportsDir.resolve("rag_cache_eval_summary.json").toFile(), report);
    }

    private TimedResult timedSearch(String question) {
        long start = System.currentTimeMillis();
        List<KnowledgeSearchHit> hits = retrievalService.hybridSearch(question, null, TOP_K);
        long latencyMs = System.currentTimeMillis() - start;
        return new TimedResult(latencyMs, hits == null ? List.of() : hits);
    }

    private void evictRetrievalCache() {
        try {
            cacheService.evictByPrefix("rag:topk:");
        } catch (Exception ignored) {
            // Cache benchmark should still run when Redis is temporarily unavailable.
        }
    }

    private Map<String, Object> buildReport(Path casesPath, int caseCount, List<Detail> details) {
        List<Long> cold = details.stream().map(Detail::coldLatencyMs).toList();
        List<Long> warm = details.stream().map(Detail::warmLatencyMs).toList();

        long coldP50 = percentile(cold, 0.50);
        long coldP95 = percentile(cold, 0.95);
        long warmP50 = percentile(warm, 0.50);
        long warmP95 = percentile(warm, 0.95);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("caseSource", casesPath.toString());
        report.put("caseCount", caseCount);
        report.put("rounds", ROUNDS);
        report.put("samples", details.size());
        report.put("scope", "Production hybrid retrieval cache. Each sample evicts rag:topk:, runs one cold query, then repeats the same query as warm cache.");
        report.put("coldP50LatencyMs", coldP50);
        report.put("warmP50LatencyMs", warmP50);
        report.put("p50ReductionPercent", reduction(coldP50, warmP50));
        report.put("coldP95LatencyMs", coldP95);
        report.put("warmP95LatencyMs", warmP95);
        report.put("p95ReductionPercent", reduction(coldP95, warmP95));
        report.put("sameTopResultsPercent", rate(details, Detail::sameTopResults));
        report.put("byQuestionType", summarizeByType(details));
        return report;
    }

    private List<Map<String, Object>> summarizeByType(List<Detail> details) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String type : List.of("direct", "synonym", "fuzzy", "followup")) {
            List<Detail> typeRows = details.stream()
                    .filter(row -> type.equals(row.questionType()))
                    .toList();
            if (typeRows.isEmpty()) {
                continue;
            }
            long coldP95 = percentile(typeRows.stream().map(Detail::coldLatencyMs).toList(), 0.95);
            long warmP95 = percentile(typeRows.stream().map(Detail::warmLatencyMs).toList(), 0.95);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("questionType", type);
            row.put("samples", typeRows.size());
            row.put("coldP95LatencyMs", coldP95);
            row.put("warmP95LatencyMs", warmP95);
            row.put("p95ReductionPercent", reduction(coldP95, warmP95));
            rows.add(row);
        }
        return rows;
    }

    private static boolean sameTopResults(List<KnowledgeSearchHit> left, List<KnowledgeSearchHit> right) {
        return signature(left).equals(signature(right));
    }

    private static String signature(List<KnowledgeSearchHit> hits) {
        return hits.stream()
                .filter(hit -> !"NEIGHBOR".equalsIgnoreCase(hit.getSource()))
                .limit(TOP_K)
                .map(hit -> nullToEmpty(hit.getDocumentId()) + ":" + nullToEmpty(hit.getChunkId()))
                .reduce("", (left, right) -> left + "|" + right);
    }

    private List<EvalCase> loadCases(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<EvalCase> cases = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> cells = parseCsvLine(lines.get(i));
            cases.add(new EvalCase(cells.get(0), cells.get(1), cells.get(2), cells.get(3)));
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
        lines.add("round,id,question_type,question,cold_latency_ms,warm_latency_ms,reduction_percent,same_top_results");
        for (Detail detail : details) {
            lines.add(String.join(",",
                    String.valueOf(detail.round()),
                    csv(detail.id()),
                    csv(detail.questionType()),
                    csv(detail.question()),
                    String.valueOf(detail.coldLatencyMs()),
                    String.valueOf(detail.warmLatencyMs()),
                    String.format(Locale.ROOT, "%.2f", reduction(detail.coldLatencyMs(), detail.warmLatencyMs())),
                    String.valueOf(detail.sameTopResults())
            ));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
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

    private static double reduction(long before, long after) {
        if (before <= 0) {
            return 0.0;
        }
        return round((before - after) * 100.0 / before);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String csv(String value) {
        return "\"" + nullToEmpty(value).replace("\"", "\"\"") + "\"";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record EvalCase(String id, String questionType, String question, String expectedDocument) {
    }

    private record TimedResult(long latencyMs, List<KnowledgeSearchHit> hits) {
    }

    private record Detail(int round, String id, String questionType, String question,
                          long coldLatencyMs, long warmLatencyMs, boolean sameTopResults) {
    }

    @FunctionalInterface
    private interface BoolMetric {
        boolean test(Detail detail);
    }
}
