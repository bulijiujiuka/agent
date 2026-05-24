package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.SensitiveWord;
import com.admin.mapper.SensitiveWordMapper;
import com.admin.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensitive-word")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordService sensitiveWordService;

    @GetMapping
    public Result<List<SensitiveWord>> list() {
        return Result.success(sensitiveWordMapper.findAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody SensitiveWord word) {
        sensitiveWordMapper.insert(word);
        sensitiveWordService.reload();
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SensitiveWord word) {
        sensitiveWordMapper.update(word);
        sensitiveWordService.reload();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveWordMapper.deleteById(id);
        sensitiveWordService.reload();
        return Result.success();
    }

    @PostMapping("/reload")
    public Result<Void> reload() {
        sensitiveWordService.reload();
        return Result.success();
    }
}
