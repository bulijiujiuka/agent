package com.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 30, message = "角色名称最长30个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 30, message = "角色编码最长30个字符")
    private String roleCode;

    @Size(max = 100, message = "描述最长100个字符")
    private String description;

    private Integer status = 1;
}
