package com.gov.security.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/修改角色请求体
 */
@Data
public class RoleRequest {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /** 数据权限：ALL / DEPT / SELF，默认 SELF */
    private String dataScope = "SELF";
}
