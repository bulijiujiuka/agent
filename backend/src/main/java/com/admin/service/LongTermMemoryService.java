package com.admin.service;

import com.admin.entity.AiLongTermMemory;
import com.admin.mapper.AiLongTermMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private static final double DEFAULT_IMPORTANCE_SCORE = 0.5;
    private static final double DEFAULT_CONFIDENCE_SCORE = 0.8;

    private final AiLongTermMemoryMapper longTermMemoryMapper;

    public AiLongTermMemory createMemory(AiLongTermMemory memory){
        validateForCreate(memory);
        fillDefaults(memory);

        longTermMemoryMapper.insert(memory);
        log.info("长期记忆已创建 id={}, type={}, sourceType={}", memory.getId(), memory.getMemoryType(), memory.getSourceType());

        return memory;
    }

    public List<AiLongTermMemory> findPendingEmbedding(int limit){
        int safeLimit = normalizeLimit(limit);
        return longTermMemoryMapper.findNotEmbedded(limit);
    }

    public void markEmbeddingProcessed(Long memoryId){
        longTermMemoryMapper.updateEmbeddingStatus(memoryId, STATUS_PROCESSING);
    }

    public void markEmbeddingCompleted(Long memoryId, String vectorId){
        longTermMemoryMapper.updateVectorStatus(memoryId, STATUS_COMPLETED, vectorId);
    }

    public void markEmbeddingFailed(Long memoryId){
        longTermMemoryMapper.updateEmbeddingStatus(memoryId, STATUS_FAILED);
    }

    public List<AiLongTermMemory> keywordRecall(String query, Long userId, int limit){
        if (query == null || query.isBlank()){
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);
        List<AiLongTermMemory> memories =
                longTermMemoryMapper.likeSearch(query.trim(), userId, safeLimit);

        memories.forEach(memory -> longTermMemoryMapper.incrementAccess(memory.getId()));
        return memories;
    }

    public List<AiLongTermMemory> fingCandidates(Long userId,
                                                 String memoryType,
                                                 String businessType,
                                                 int limit){
        int safeLimit = normalizeLimit(limit);
        return longTermMemoryMapper.findCandidates(userId, memoryType, businessType, safeLimit);
    }

    public void disableMemory(Long memoryId){
        longTermMemoryMapper.updateEnabled(memoryId, false);
    }

    public void enableMemory(Long memoryId){
        longTermMemoryMapper.updateEnabled(memoryId, true);
    }

    public void validateForCreate(AiLongTermMemory memory){
        if (memory == null){
            throw new RuntimeException("长期记忆不能为空");
        }
        if (memory.getMemoryType() == null || memory.getMemoryType().isBlank()){
            throw new RuntimeException("长期记忆类型不能为空");
        }
        if (memory.getMemoryText() == null || memory.getMemoryText().isBlank()){
            throw new RuntimeException("长期记忆内容不能为空");
        }
        if (memory.getSourceType() == null || memory.getSourceType().isBlank()){
            throw new RuntimeException("长期记忆来源不能为空");
        }
    }

    public void fillDefaults(AiLongTermMemory memory){
        if (memory.getBusinessType() == null || memory.getBusinessType().isBlank()){
            memory.setBusinessType("QA");
        }
        if (memory.getImportanceScore() == null){
            memory.setImportanceScore(DEFAULT_IMPORTANCE_SCORE);
        }
        if (memory.getConfidenceScore() == null){
            memory.setConfidenceScore(DEFAULT_CONFIDENCE_SCORE);
        }
        if (memory.getAccessCount() == null){
            memory.setAccessCount(0);
        }
        if (memory.getEmbeddingStatus() == null || memory.getEmbeddingStatus().isBlank()){
            memory.setEmbeddingStatus(STATUS_PENDING);
        }
        if (memory.getEnabled() == null){
            memory.setEnabled(true);
        }
    }

    private int normalizeLimit(int limit){
        if (limit <= 0){
            return 10;
        }
        return Math.min(limit, 100);
    }
}
