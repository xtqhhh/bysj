package com.gov.security.controller;

import com.gov.security.common.annotation.Auditable;
import com.gov.security.common.response.ApiResponse;
import com.gov.security.controller.dto.AssignPermissionsRequest;
import com.gov.security.controller.dto.CreateUserRequest;
import com.gov.security.controller.dto.ResetPasswordRequest;
import com.gov.security.controller.dto.RoleRequest;
import com.gov.security.controller.dto.UpdateUserStatusRequest;
import com.gov.security.entity.Role;
import com.gov.security.service.AuditLogService;
import com.gov.security.service.RoleService;
import com.gov.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 所有接口仅限 ROLE_SUPER_ADMIN 访问（需求：6.3、6.4）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final RoleService roleService;

    /**
     * GET /api/admin/users
     * 分页查询用户列表（支持按用户名、角色、状态过滤）
     * 需求：9.1
     */
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.listUsers(username, role, status, page, size));
    }

    /**
     * POST /api/admin/users
     * 创建用户（指定用户名、密码、角色、部门）
     * 需求：9.2
     */
    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest req) {
        Long id = userService.createUser(req);
        return ApiResponse.success("创建成功", Map.of("id", id, "message", "创建成功"));
    }

    /**
     * PUT /api/admin/users/{id}/roles
     * 修改用户角色（提权/降权），同时清除权限缓存使变更立即生效
     */
    @PutMapping("/users/{id}/roles")
    @Auditable(operationType = "ROLE_CHANGE")
    public ApiResponse<Void> updateUserRoles(@PathVariable Long id,
                                              @RequestBody UpdateUserRolesRequest req) {
        userService.updateUserRoles(id, req.getRoleIds());
        return ApiResponse.success("角色修改成功");
    }

    @lombok.Data
    public static class UpdateUserRolesRequest {
        private java.util.List<Long> roleIds;
    }

    /**
     * PUT /api/admin/users/{id}/status
     * 禁用/启用用户，禁用时使该用户所有在线 Token 立即失效
     * 需求：9.3
     */
    @PutMapping("/users/{id}/status")
    @Auditable(operationType = "USER_DISABLE")
    public ApiResponse<Map<String, String>> updateUserStatus(@PathVariable Long id,
                                                              @Valid @RequestBody UpdateUserStatusRequest req) {
        userService.updateUserStatus(id, req.getStatus());
        return ApiResponse.success(Map.of("message", "操作成功"));
    }

    /**
     * PUT /api/admin/users/{id}/password
     * 重置用户密码，BCrypt 加密后更新，同时使该用户所有在线 Token 失效
     * 需求：9.4
     */
    @PutMapping("/users/{id}/password")
    public ApiResponse<Map<String, String>> resetUserPassword(@PathVariable Long id,
                                                               @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetUserPassword(id, req.getNewPassword());
        return ApiResponse.success(Map.of("message", "密码重置成功"));
    }

    // ---- 角色管理 ----

    /**
     * GET /api/admin/roles
     * 查询角色列表
     * 需求：9.5
     */
    @GetMapping("/roles")
    public ApiResponse<java.util.List<Role>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    /**
     * POST /api/admin/roles
     * 创建角色
     * 需求：9.5
     */
    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> createRole(@Valid @RequestBody RoleRequest req) {
        Long id = roleService.createRole(req.getRoleName(), req.getRoleCode(), req.getDataScope());
        return ApiResponse.success("创建成功", Map.of("id", id));
    }

    /**
     * PUT /api/admin/roles/{id}
     * 修改角色
     * 需求：9.5
     */
    @PutMapping("/roles/{id}")
    public ApiResponse<Void> updateRole(@PathVariable Long id,
                                         @Valid @RequestBody RoleRequest req) {
        roleService.updateRole(id, req.getRoleName(), req.getRoleCode(), req.getDataScope());
        return ApiResponse.success("操作成功");
    }

    /**
     * DELETE /api/admin/roles/{id}
     * 删除角色（校验是否有关联用户，若有则返回 ROLE_IN_USE）
     * 需求：9.5
     */
    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success("删除成功");
    }

    /**
     * PUT /api/admin/roles/{id}/permissions
     * 为角色分配权限，同时清除该角色下所有用户的权限缓存
     * 需求：9.6、6.5
     */
    @PutMapping("/roles/{id}/permissions")
    @Auditable(operationType = "PERMISSION_CHANGE")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                                @Valid @RequestBody AssignPermissionsRequest req) {
        roleService.assignPermissions(id, req.getPermissionIds());
        return ApiResponse.success("权限分配成功");
    }

    // ---- 审计日志 ----

    /**
     * GET /api/admin/audit-logs
     * 分页查询审计日志
     * 需求：10.4、10.5
     */
    @GetMapping("/audit-logs")
    public ApiResponse<Map<String, Object>> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(auditLogService.findPage(operatorId, type, startTime, endTime, page, size));
    }
}
