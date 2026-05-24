package com.admin.service;

import com.admin.dto.RoleRequest;
import com.admin.entity.Role;
import com.admin.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }

    public List<Role> getAllRoles() {
        return roleMapper.findAll();
    }

    public Role getRoleById(Long id) {
        return roleMapper.findById(id);
    }

    public int createRole(RoleRequest request) {
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        return roleMapper.insert(role);
    }

    public int updateRole(RoleRequest request) {
        Role role = new Role();
        role.setId(request.getId());
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        return roleMapper.update(role);
    }

    public int deleteRole(Long id) {
        return roleMapper.deleteById(id);
    }
}
