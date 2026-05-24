package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.KbCategory;
import com.admin.mapper.KbCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kb/category")
@RequiredArgsConstructor
public class KbCategoryController {

    private final KbCategoryMapper categoryMapper;

    @GetMapping
    public Result<List<KbCategory>> list() {
        return Result.success(categoryMapper.findAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody KbCategory category) {
        categoryMapper.insert(category);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody KbCategory category) {
        categoryMapper.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
