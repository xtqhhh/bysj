package com.gov.security.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 为角色分配权限请求体
 */
@Data
public class AssignPermissionsRequest {

    @NotNull(message = "权限ID列表不能为null")
    private List<Long> permissionIds;
}
