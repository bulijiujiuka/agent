package com.admin.service;

import com.admin.dto.KnowledgeSearchHit;
import com.admin.mapper.KbChunkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KbChunkMapper chunkMapper;
    private final TwoLevelCacheService cacheService;

    @Value("${ai.rag.hybrid.vector-top-k:10}")
    private int vectorTopK;

    @Value("${ai.rag.hybrid.keyword-top-k:10}")
    private int keywordTopK;

    @Value("${ai.rag.hybrid.final-top-k:5}")
    private int finalTopK;

    @Value("${ai.rag.hybrid.vector-weight:0.65}")
    private double vectorWeight;

    @Value("${ai.rag.hybrid.keyword-weight:0.35}")
    private double keywordWeight;

    @Value("${ai.rag.hybrid.min-vector-score:0.3}")
    private double minVectorScore;

    public List<KnowledgeSearchHit> hybridSearch(String query) {
        return hybridSearch(query, null, finalTopK);
    }

    public List<KnowledgeSearchHit> hybridSearch(String query, Long documentId, int limit) {
        String key = "rag:topk:" +(documentId == null ? "all" : documentId) + ":" + limit + ":" + query;
        return cacheService.get(
                key,
                new TypeReference<List<KnowledgeSearchHit>>() {},
                Duration.ofMinutes(10),
                Duration.ofMinutes(2),
                () -> dohybridSearch(query, documentId, limit)
        );
    }

    public List<KnowledgeSearchHit> searchForEvaluation(String query, Long documentId, int limit,
                                                        String retrievalMode, boolean expandNeighbors,
                                                        boolean useCache) {
        String mode = retrievalMode == null || retrievalMode.isBlank()
                ? "HYBRID"
                : retrievalMode.trim().toUpperCase(Locale.ROOT);

        if (useCache && "HYBRID".equals(mode) && expandNeighbors) {
            return hybridSearch(query, documentId, limit);
        }

        if (!useCache) {
            return doSearch(query, documentId, limit, mode, expandNeighbors);
        }

        String key = "rag:eval:" + mode + ":" + expandNeighbors + ":"
                + (documentId == null ? "all" : documentId) + ":" + limit + ":" + query;
        return cacheService.get(
                key,
                new TypeReference<List<KnowledgeSearchHit>>() {},
                Duration.ofMinutes(10),
                Duration.ofMinutes(2),
                () -> doSearch(query, documentId, limit, mode, expandNeighbors)
        );
    }

    private List<KnowledgeSearchHit> doSearch(String query, Long documentId, int limit,
                                              String mode, boolean expandNeighbors) {
        if ("VECTOR".equals(mode)) {
            int actualLimit = limit > 0 ? limit : finalTopK;
            List<KnowledgeSearchHit> ranked = vectorSearch(query, documentId).stream()
                    .peek(hit -> {
                        hit.setSource("VECTOR");
                        hit.setFinalScore(hit.getVectorScore());
                    })
                    .sorted(Comparator.comparing(
                            KnowledgeSearchHit::getFinalScore,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .limit(actualLimit)
                    .toList();
            return expandNeighbors ? expandWithNeighbors(ranked) : ranked;
        }
        return dohybridSearch(query, documentId, limit, expandNeighbors);
    }

    public List<KnowledgeSearchHit> dohybridSearch(String query, Long documentId, int limit) {
        return dohybridSearch(query, documentId, limit, true);
    }

    private List<KnowledgeSearchHit> dohybridSearch(String query, Long documentId, int limit, boolean expandNeighbors) {
        List<KnowledgeSearchHit> vectorHits = vectorSearch(query, documentId);
        List<KnowledgeSearchHit> keywordHits = keywordSearch(query, documentId);

        double maxVectorScore = vectorHits.stream()
                .map(KnowledgeSearchHit::getVectorScore)
                .filter(score -> score != null)
                .max(Double::compareTo)
                .orElse(0.0);

        double maxKeywordScore = keywordHits.stream()
                .map(KnowledgeSearchHit::getBm25Score)
                .filter(score -> score != null)
                .max(Double::compareTo)
                .orElse(0.0);

        Map<String, KnowledgeSearchHit> merged = new LinkedHashMap<>();

        for (KnowledgeSearchHit hit : vectorHits) {
            hit.setSource("VECTOR");
            merged.put(hit.getChunkId(), hit);
        }

        for (KnowledgeSearchHit keywordHit : keywordHits) {
            KnowledgeSearchHit existing = merged.get(keywordHit.getChunkId());
            if (existing == null) {
                keywordHit.setSource("BM25");
                merged.put(keywordHit.getChunkId(), keywordHit);
            } else {
                existing.setBm25Score(keywordHit.getBm25Score());
                existing.setSource("HYBRID");
                if (existing.getText() == null || existing.getText().isBlank()) {
                    existing.setText(keywordHit.getText());
                }
                if (existing.getDocumentName() == null || existing.getDocumentName().isBlank()) {
                    existing.setDocumentName(keywordHit.getDocumentName());
                }
            }
        }

        int actualLimit = limit > 0 ? limit : finalTopK;
        List<KnowledgeSearchHit> ranked= merged.values().stream()
                .peek(hit -> calculateFinalScore(hit, maxVectorScore, maxKeywordScore))
                .sorted(Comparator.comparing(
                        KnowledgeSearchHit::getFinalScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(actualLimit)
                .toList();
        return expandNeighbors ? expandWithNeighbors(ranked) : ranked;
    }

    private List<KnowledgeSearchHit> expandWithNeighbors(List<KnowledgeSearchHit> hits) {
        Map<String,KnowledgeSearchHit> expanded = new LinkedHashMap<>();

        for (KnowledgeSearchHit hit : hits) {
            expanded.put(hit.getChunkId(), hit);

            Long docId = Long.valueOf(hit.getDocumentId());
            int index = Integer.parseInt(hit.getChunkIndex());

            for (var chunk : chunkMapper.findWindow(docId, Math.max(0, index - 1), index + 1)){
                String key = chunk.getId().toString();
                if (expanded.containsKey(key)) continue;

                KnowledgeSearchHit neighborHit = new KnowledgeSearchHit();
                neighborHit.setChunkId(key);
                neighborHit.setDocumentId(hit.getDocumentId());
                neighborHit.setDocumentName(hit.getDocumentName());
                neighborHit.setChunkIndex(chunk.getChunkIndex().toString());
                neighborHit.setText(chunk.getChunkText());
                neighborHit.setSource("NEIGHBOR");
                neighborHit.setCategory(hit.getCategory());
                neighborHit.setSectionTitle(chunk.getSectionTitle());
                neighborHit.setTitlePath(chunk.getTitlePath());
                neighborHit.setVersion(hit.getVersion());
                neighborHit.setFinalScore(hit.getFinalScore());
                expanded.put(key, neighborHit);
            }
        }
        return new ArrayList<>(expanded.values());
    }

    private List<KnowledgeSearchHit> vectorSearch(String query, Long documentId) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            var builder = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(vectorTopK)
                    .minScore(minVectorScore);

            if (documentId != null) {
                builder.filter(
                        MetadataFilterBuilder.metadataKey("documentId")
                                .isEqualTo(documentId.toString())
                );
            }

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(builder.build());
            return result.matches().stream()
                    .map(this::toVectorHit)
                    .filter(hit -> hit.getChunkId() != null && !hit.getChunkId().isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("Vector search failed, query={}, documentId={}, error={}", query, documentId, e.getMessage());
            return List.of();
        }
    }

    private KnowledgeSearchHit toVectorHit(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();

        KnowledgeSearchHit hit = new KnowledgeSearchHit();
        hit.setChunkId(segment.metadata().getString("chunkId"));
        hit.setDocumentId(segment.metadata().getString("documentId"));
        hit.setDocumentName(segment.metadata().getString("documentName"));
        hit.setChunkIndex(segment.metadata().getString("chunkIndex"));
        hit.setText(segment.text());
        hit.setVectorScore(match.score());
        hit.setSource("VECTOR");
        return hit;
    }

    private List<KnowledgeSearchHit> keywordSearch(String query, Long documentId) {
        try {
            List<KnowledgeSearchHit> hits = chunkMapper.fulltextSearch(query, documentId, keywordTopK);
            if (hits != null && !hits.isEmpty()) {
                return hits;
            }
        } catch (Exception e) {
            log.warn("FULLTEXT search failed, fallback to LIKE, query={}, documentId={}, error={}",
                    query, documentId, e.getMessage());
        }

        try {
            List<KnowledgeSearchHit> hits = chunkMapper.likeSearch(query, documentId, keywordTopK);
            return hits == null ? List.of() : hits;
        } catch (Exception e) {
            log.warn("LIKE search failed, query={}, documentId={}, error={}", query, documentId, e.getMessage());
            return List.of();
        }
    }

    private void calculateFinalScore(KnowledgeSearchHit hit, double maxVectorScore, double maxKeywordScore) {
        double vectorNorm = normalize(hit.getVectorScore(), maxVectorScore);
        double keywordNorm = normalize(hit.getBm25Score(), maxKeywordScore);
        hit.setFinalScore(vectorWeight * vectorNorm + keywordWeight * keywordNorm);
    }

    private double normalize(Double score, double maxScore) {
        if (score == null || maxScore <= 0) {
            return 0.0;
        }
        return Math.min(1.0, score / maxScore);
    }
}
