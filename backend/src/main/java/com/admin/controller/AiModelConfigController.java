package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.AiModelConfig;
import com.admin.mapper.AiModelConfigMapper;
import com.admin.service.TwoLevelCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/model-config")
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiModelConfigMapper modelConfigMapper;
    private final TwoLevelCacheService cacheService;

    private void evictModelCaches() {
        cacheService.evictByPrefix("config:model:");

        // 模型/baseUrl/apiKey/默认模型变化后，旧问答缓存建议清掉
        cacheService.evictByPrefix("qa:normal:");
        cacheService.evictByPrefix("qa:stream:");
        cacheService.evictByPrefix("qa:agent:");
    }

    @GetMapping
    public Result<List<AiModelConfig>> list() {
        return Result.success(modelConfigMapper.findAll());
    }

    @GetMapping("/enabled")
    public Result<List<AiModelConfig>> listEnabled() {
        return Result.success(modelConfigMapper.findAllEnabled());
    }

    @PostMapping
    public Result<Void> create(@RequestBody AiModelConfig config) {
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            modelConfigMapper.clearDefault();
        }
        modelConfigMapper.insert(config);
        evictModelCaches();
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody AiModelConfig config) {
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            modelConfigMapper.clearDefault();
        }
        modelConfigMapper.update(config);
        evictModelCaches();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigMapper.deleteById(id);
        evictModelCaches();
        return Result.success();
    }
}
