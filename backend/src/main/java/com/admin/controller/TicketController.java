package com.admin.controller;

import com.admin.annotation.OperLog;
import com.admin.dto.PageResult;
import com.admin.dto.Result;
import com.admin.dto.TicketQueryRequest;
import com.admin.entity.Ticket;
import com.admin.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/list")
    public Result<List<Ticket>> list() {
        return Result.success(ticketService.getAll());
    }

    @GetMapping("/page")
    public Result<PageResult<Ticket>> page(TicketQueryRequest query) {
        return Result.success(ticketService.getPage(query));
    }

    @GetMapping("/{id}")
    public Result<Ticket> getById(@PathVariable Long id) {
        return Result.success(ticketService.getById(id));
    }

    @OperLog(module = "工单管理", description = "新增工单")
    @PostMapping
    public Result<Ticket> create(@RequestBody Ticket ticket) {
        return Result.success(ticketService.create(ticket));
    }

    @OperLog(module = "工单管理", description = "修改工单")
    @PutMapping
    public Result<Void> update(@RequestBody Ticket ticket) {
        ticketService.update(ticket);
        return Result.success();
    }

    @OperLog(module = "工单管理", description = "删除工单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }

    @OperLog(module = "工单管理", description = "重新生成AI辅助")
    @PostMapping("/{id}/ai-assist")
    public Result<String> regenerateAiAssist(@PathVariable Long id) {
        Ticket ticket = ticketService.getById(id);
        if (ticket == null) {
            return Result.error("工单不存在");
        }
        java.util.concurrent.CompletableFuture.runAsync(() ->
                ticketService.generateAiAssist(ticket.getId(), ticket.getTitle(), ticket.getContent()));
        return Result.success("AI 辅助正在后台重新生成，请稍后刷新查看");
    }
}
