package com.admin.service;

import com.admin.entity.AiCallLog;
import com.admin.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    private final AiCallLogMapper callLogMapper;

    public String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Async
    public void log(String traceId, Long conversationId, Long messageId,
                    String stepName, String modelKey,
                    Integer inputLength, Integer outputLength,
                    int durationMs, boolean success, String errorMsg, String extra) {
        try {
            AiCallLog entry = new AiCallLog();
            entry.setTraceId(traceId);
            entry.setConversationId(conversationId);
            entry.setMessageId(messageId);
            entry.setStepName(stepName);
            entry.setModelKey(modelKey);
            entry.setInputLength(inputLength);
            entry.setOutputLength(outputLength);
            entry.setDurationMs(durationMs);
            entry.setSuccess(success);
            entry.setErrorMsg(errorMsg);
            entry.setExtra(extra);
            callLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写入调用链日志失败: {}", e.getMessage());
        }
    }

    public void logStep(String traceId, Long conversationId, String stepName,
                        int inputLen, int outputLen, int durationMs, boolean success, String errorMsg) {
        log(traceId, conversationId, null, stepName, null, inputLen, outputLen, durationMs, success, errorMsg, null);
    }
}
