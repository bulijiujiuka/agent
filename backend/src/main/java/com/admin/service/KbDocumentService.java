package com.admin.service;

import com.admin.dto.KbDocumentQueryRequest;
import com.admin.dto.PageResult;
import com.admin.entity.KbChunk;
import com.admin.entity.KbDocument;
import com.admin.exception.BusinessException;
import com.admin.mapper.KbChunkMapper;
import com.admin.mapper.KbDocumentMapper;
import com.admin.mapper.KbDocumentVersionMapper;
import com.admin.entity.KbDocumentVersion;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentService {

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final KbDocumentVersionMapper versionMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TwoLevelCacheService cacheService;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private void evictKnowledgeCaches() {
        // 检索 TopK 结果依赖知识库 chunk、向量、文档启用状态
        cacheService.evictByPrefix("rag:topk:");

        // 工具里的文档列表和系统统计依赖 kb_document
        cacheService.evict("tool:docs:list");
        cacheService.evict("tool:system:stats");

        // 已生成问答可能基于旧知识库，文档变更后建议清掉
        cacheService.evictByPrefix("qa:normal:");
        cacheService.evictByPrefix("qa:stream:");
        cacheService.evictByPrefix("qa:agent:");
    }

    public List<KbDocument> getAll() {
        return documentMapper.findAll();
    }

    public KbDocument getById(Long id) {
        return documentMapper.findById(id);
    }

    public PageResult<KbDocument> getPage(KbDocumentQueryRequest query) {
        long total = documentMapper.countByCondition(
                query.getDocumentName(), query.getCategory(), query.getParseStatus());
        List<KbDocument> records = documentMapper.findByPage(
                query.getDocumentName(), query.getCategory(), query.getParseStatus(),
                query.getOffset(), query.getPageSize());
        return PageResult.of(records, total, query);
    }

    public KbDocument create(KbDocument document) {
        document.setParseStatus("PENDING");
        document.setEmbeddingStatus("PENDING");
        document.setChunkCount(0);
        document.setEnabled(true);
        document.setVersion(1);
        documentMapper.insert(document);

        evictKnowledgeCaches();
        createVersionSnapshot(document);
        return document;
    }

    public KbDocument uploadAndParse(MultipartFile file, String category, String uploadedBy) {
        String originalName = file.getOriginalFilename();

        KbDocument doc = new KbDocument();
        doc.setDocumentName(originalName);
        doc.setSourceType("MANUAL");
        doc.setCategory(category);
        doc.setUploadedBy(uploadedBy);
        doc.setParseStatus("PROCESSING");
        doc.setEmbeddingStatus("PENDING");
        doc.setChunkCount(0);
        doc.setEnabled(true);
        doc.setVersion(1);
        documentMapper.insert(doc);

        evictKnowledgeCaches();

        parseAndIndex(doc.getId(), file);
        return doc;
    }

    @Transactional
    public KbDocument reuploadAndParse(Long id, MultipartFile file, String category) {
        KbDocument existing = documentMapper.findById(id);
        if (existing == null) {
            throw new BusinessException("文档不存在");
        }
        createVersionSnapshot(existing);
        removeDocumentVectors(id);
        chunkMapper.deleteByDocumentId(id);
        existing.setDocumentName(file.getOriginalFilename());
        if (category != null) existing.setCategory(category);
        existing.setParseStatus("PROCESSING");
        existing.setEmbeddingStatus("PENDING");
        existing.setChunkCount(0);
        existing.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 1);
        documentMapper.update(existing);

        evictKnowledgeCaches();

        parseAndIndex(id, file);
        return existing;
    }

    @Async
    public void parseAndIndex(Long documentId, MultipartFile file) {
        try {
            String text = readFileText(file);
            if (text.isBlank()) {
                updateStatus(documentId, "FAILED", "PENDING", 0);
                return;
            }

            List<SemanticChunkService.ChunkBlock> chunks = SemanticChunkService.split(text, CHUNK_SIZE, CHUNK_OVERLAP);
            int index = 0;
            for (SemanticChunkService.ChunkBlock block : chunks) {
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(index++);
                chunk.setChunkText(block.text());
                chunk.setSectionTitle(block.sectionTitle());
                chunk.setTitlePath(block.titlePath());
                chunk.setCharCount(block.text().length());
                chunk.setVectorStatus("PENDING");
                chunkMapper.insert(chunk);
            }

            updateStatus(documentId, "COMPLETED", "PROCESSING", chunks.size());

            int vectorized = 0;
            int failed = 0;
            KbDocument parsedDoc = documentMapper.findById(documentId);
            for (KbChunk chunk : chunkMapper.findByDocumentId(documentId)) {
                try {
                    chunkMapper.updateVectorStatus(chunk.getId(), "PROCESSING");

                    dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
                    metadata.put("documentId", documentId.toString());
                    metadata.put("documentName", parsedDoc != null ? parsedDoc.getDocumentName() : "未知");
                    metadata.put("chunkId", chunk.getId().toString());
                    metadata.put("chunkIndex", chunk.getChunkIndex().toString());
                    if (parsedDoc != null){
                        metadata.put("category", parsedDoc.getCategory() == null ? "" : parsedDoc.getCategory());
                        metadata.put("version", parsedDoc.getVersion() == null ? "1" : parsedDoc.getVersion().toString());
                    }else {
                        metadata.put("category", "");
                        metadata.put("version", "1");
                    }

                    metadata.put("sectionTitle", chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle());
                    metadata.put("titlePath", chunk.getTitlePath() == null ? "" : chunk.getTitlePath());

                    TextSegment segment = TextSegment.from(chunk.getChunkText(), metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    embeddingStore.add(embedding, segment);

                    chunkMapper.updateVectorStatus(chunk.getId(), "COMPLETED");
                    vectorized++;
                } catch (Exception e) {
                    failed++;
                    chunkMapper.updateVectorStatus(chunk.getId(), "FAILED");
                    log.warn("切片 {} 向量化失败: {}", chunk.getId(), e.getMessage());
                }
            }

            updateStatus(documentId, "COMPLETED", failed >0 ? "FAILED" : "COMPLETED", chunks.size());

            evictKnowledgeCaches();
            if (versionMapper.findByDocumentId(documentId).isEmpty()) {
                KbDocument parsed = documentMapper.findById(documentId);
                if (parsed != null) {
                    createVersionSnapshot(parsed);
                }
            }
            log.info("文档 {} 解析完成：{} 个切片，{} 个已向量化", documentId, chunks.size(), vectorized);

        } catch (Exception e) {
            log.error("文档 {} 解析失败: {}", documentId, e.getMessage());
            updateStatus(documentId, "FAILED", "FAILED", 0);
        }
    }

    private void updateStatus(Long documentId, String parseStatus, String embeddingStatus, int chunkCount) {
        KbDocument existing = documentMapper.findById(documentId);
        if (existing != null) {
            existing.setParseStatus(parseStatus);
            existing.setEmbeddingStatus(embeddingStatus);
            existing.setChunkCount(chunkCount);
            documentMapper.update(existing);
        }
    }

    private String readFileText(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("读取文件文本失败: {}", e.getMessage());
            return "";
        }
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }

    @Transactional
    public int update(KbDocument document) {
        KbDocument existing = documentMapper.findById(document.getId());
        if (existing == null) {
            throw new BusinessException("文档不存在");
        }
        createVersionSnapshot(existing);
        documentMapper.incrementVersion(document.getId());
        if (document.getDocumentName() != null) existing.setDocumentName(document.getDocumentName());
        if (document.getCategory() != null) existing.setCategory(document.getCategory());
        if (document.getTags() != null) existing.setTags(document.getTags());
        if (document.getSummary() != null) existing.setSummary(document.getSummary());
        int rows = documentMapper.update(existing);
        evictKnowledgeCaches();
        return rows;
    }

    @Transactional
    public void delete(Long id) {
        KbDocument doc = documentMapper.findById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        removeDocumentVectors(id);
        versionMapper.deleteByDocumentId(id);
        chunkMapper.deleteByDocumentId(id);
        documentMapper.deleteById(id);
        evictKnowledgeCaches();
    }

    public List<KbChunk> getChunks(Long documentId) {
        return chunkMapper.findByDocumentId(documentId);
    }

    public void toggleEnabled(Long id, boolean enabled) {
        documentMapper.updateEnabled(id, enabled);
        if (enabled) {
            indexDocumentVectors(id);
        } else {
            removeDocumentVectors(id);
        }
        evictKnowledgeCaches();
    }

    private void removeDocumentVectors(Long documentId) {
        try {
            embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documentId").isEqualTo(documentId.toString()));
            log.info("已移除文档 {} 的向量索引", documentId);
        } catch (Exception e) {
            log.warn("移除文档 {} 向量索引失败: {}", documentId, e.getMessage());
        }
    }

    private void indexDocumentVectors(Long documentId) {
        KbDocument doc = documentMapper.findById(documentId);
        if (doc == null) return;

        removeDocumentVectors(documentId);

        var chunks = chunkMapper.findByDocumentId(documentId);
        int count = 0;
        for (var chunk : chunks) {
            try {
                dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
                metadata.put("documentId", doc.getId().toString());
                metadata.put("documentName", doc.getDocumentName());
                metadata.put("chunkId", chunk.getId().toString());
                metadata.put("chunkIndex", chunk.getChunkIndex().toString());
                TextSegment segment = TextSegment.from(chunk.getChunkText(), metadata);
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
                count++;
            } catch (Exception e) {
                log.warn("文档 {} 切片 {} 向量化失败: {}", documentId, chunk.getChunkIndex(), e.getMessage());
            }
        }
        log.info("已为文档 {} 补建向量索引 ({} 个切片)", documentId, count);
        cacheService.evictByPrefix("rag:topk:");
        cacheService.evict("tool:docs:list");
    }

    public List<KbDocumentVersion> getVersions(Long documentId) {
        return versionMapper.findByDocumentId(documentId);
    }

    public KbDocumentVersion getVersion(Long documentId, int version) {
        KbDocumentVersion ver = versionMapper.findByDocumentIdAndVersion(documentId, version);
        if (ver == null) {
            throw new BusinessException("版本不存在");
        }
        return ver;
    }

    @Transactional
    public void rollbackVersion(Long documentId, int targetVersion) {
        KbDocumentVersion ver = versionMapper.findByDocumentIdAndVersion(documentId, targetVersion);
        if (ver == null) {
            throw new BusinessException("版本不存在");
        }
        KbDocument current = documentMapper.findById(documentId);
        if (current == null) {
            throw new BusinessException("文档不存在");
        }
        createVersionSnapshot(current);
        current.setDocumentName(ver.getDocumentName());
        current.setCategory(ver.getCategory());
        current.setTags(ver.getTags());
        current.setSummary(ver.getSummary());
        documentMapper.update(current);
        documentMapper.incrementVersion(documentId);
        evictKnowledgeCaches();
    }

    private void createVersionSnapshot(KbDocument doc) {
        KbDocumentVersion ver = new KbDocumentVersion();
        ver.setDocumentId(doc.getId());
        ver.setVersion(doc.getVersion() != null ? doc.getVersion() : 1);
        ver.setDocumentName(doc.getDocumentName());
        ver.setCategory(doc.getCategory());
        ver.setTags(doc.getTags());
        ver.setSummary(doc.getSummary());
        StringBuilder contentSnapshot = new StringBuilder();
        for (KbChunk chunk : chunkMapper.findByDocumentId(doc.getId())) {
            contentSnapshot.append(chunk.getChunkText()).append("\n");
        }
        ver.setContentSnapshot(contentSnapshot.toString());
        ver.setCreatedBy(doc.getUploadedBy());
        versionMapper.insert(ver);
    }
}
