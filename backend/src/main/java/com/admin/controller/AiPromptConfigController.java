package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.AiPromptConfig;
import com.admin.mapper.AiPromptConfigMapper;
import com.admin.service.TwoLevelCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/prompt-config")
@RequiredArgsConstructor
public class AiPromptConfigController {

    private final AiPromptConfigMapper promptConfigMapper;
    private final TwoLevelCacheService cacheService;

    private void evictPromptCaches() {
        cacheService.evictByPrefix("config:prompt:");

        // Prompt 改了，旧答案风格/规则可能不再符合新配置
        cacheService.evictByPrefix("qa:normal:");
        cacheService.evictByPrefix("qa:stream:");
        cacheService.evictByPrefix("qa:agent:");
    }

    @GetMapping
    public Result<List<AiPromptConfig>> list() {
        return Result.success(promptConfigMapper.findAll());
    }

    @GetMapping("/{id}")
    public Result<AiPromptConfig> getById(@PathVariable Long id) {
        return Result.success(promptConfigMapper.findById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody AiPromptConfig config) {
        promptConfigMapper.insert(config);
        evictPromptCaches();
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody AiPromptConfig config) {
        promptConfigMapper.update(config);
        evictPromptCaches();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptConfigMapper.deleteById(id);
        evictPromptCaches();
        return Result.success();
    }
}
