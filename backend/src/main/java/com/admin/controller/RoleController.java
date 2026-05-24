package com.admin.controller;

import com.admin.annotation.OperLog;
import com.admin.dto.Result;
import com.admin.dto.RoleRequest;
import com.admin.entity.Role;
import com.admin.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @OperLog(module = "角色管理", description = "新增角色")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleRequest request) {
        roleService.createRole(request);
        return Result.success();
    }

    @OperLog(module = "角色管理", description = "修改角色")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleRequest request) {
        roleService.updateRole(request);
        return Result.success();
    }

    @OperLog(module = "角色管理", description = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
