package com.admin.controller;

import com.admin.dto.PageRequest;
import com.admin.dto.PageResult;
import com.admin.dto.Result;
import com.admin.entity.OperLog;
import com.admin.mapper.OperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oper-log")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogMapper operLogMapper;

    @GetMapping("/page")
    public Result<PageResult<OperLog>> page(@RequestParam(required = false) String module,
                                             @RequestParam(required = false) String operUser,
                                             PageRequest pageRequest) {
        long total = operLogMapper.count(module, operUser);
        var records = operLogMapper.findByPage(module, operUser, pageRequest.getOffset(), pageRequest.getPageSize());
        return Result.success(PageResult.of(records, total, pageRequest));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        operLogMapper.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operLogMapper.clear();
        return Result.success();
    }
}
