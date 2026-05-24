package com.admin.service;

import com.admin.entity.SensitiveWord;
import com.admin.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final CopyOnWriteArrayList<SensitiveWord> cachedWords = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void reload() {
        cachedWords.clear();
        cachedWords.addAll(sensitiveWordMapper.findAllEnabled());
        log.info("已加载 {} 个敏感词", cachedWords.size());
    }

    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        for (SensitiveWord sw : cachedWords) {
            result = result.replace(sw.getWord(), sw.getReplacement());
        }
        return result;
    }

    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) return false;
        for (SensitiveWord sw : cachedWords) {
            if (text.contains(sw.getWord())) return true;
        }
        return false;
    }
}
