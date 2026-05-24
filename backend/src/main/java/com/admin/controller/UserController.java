package com.admin.controller;

import com.admin.annotation.OperLog;
import com.admin.dto.*;
import com.admin.entity.User;
import com.admin.service.UserService;
import com.admin.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/page")
    public Result<PageResult<User>> page(UserQueryRequest query) {
        return Result.success(userService.getUserPage(query));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @OperLog(module = "用户管理", description = "新增用户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return Result.success();
    }

    @OperLog(module = "用户管理", description = "修改用户")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(request);
        return Result.success();
    }

    @OperLog(module = "用户管理", description = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @OperLog(module = "用户管理", description = "分配角色")
    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @OperLog(module = "用户管理", description = "重置密码")
    @PutMapping("/{id}/resetPassword")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        userService.resetPassword(id, body.get("newPassword"));
        return Result.success();
    }

    @OperLog(module = "用户管理", description = "导出用户")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<User> users = userService.getAllUsers();
        List<UserExcel> excelList = users.stream().map(u -> {
            UserExcel e = new UserExcel();
            e.setUsername(u.getUsername());
            e.setNickname(u.getNickname());
            e.setEmail(u.getEmail());
            e.setPhone(u.getPhone());
            e.setStatusText(u.getStatus() == 1 ? "启用" : "禁用");
            e.setRoles(u.getRoleNames() != null ? String.join(",", u.getRoleNames()) : "");
            e.setCreateTime(u.getCreateTime() != null ? u.getCreateTime().format(fmt) : "");
            return e;
        }).toList();
        ExcelUtil.export(response, "用户列表", "用户", UserExcel.class, excelList);
    }
}
