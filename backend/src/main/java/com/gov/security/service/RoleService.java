package com.gov.security.service;

import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.entity.Role;
import com.gov.security.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色业务服务
 * 需求：9.5、9.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserService userService;

    /**
     * 查询所有角色列表
     * 需求：9.5
     */
    public List<Role> listRoles() {
        return roleMapper.findAll();
    }

    /**
     * 创建角色
     * 需求：9.5
     */
    @Transactional
    public Long createRole(String roleName, String roleCode, String dataScope) {
        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setDataScope(dataScope != null ? dataScope : "SELF");
        roleMapper.insert(role);
        log.info("创建角色成功: roleCode={}", roleCode);
        return role.getId();
    }

    /**
     * 修改角色
     * 需求：9.5
     */
    @Transactional
    public void updateRole(Long id, String roleName, String roleCode, String dataScope) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setDataScope(dataScope != null ? dataScope : "SELF");
        roleMapper.update(role);
        log.info("修改角色成功: id={}", id);
    }

    /**
     * 删除角色，若有关联用户则抛出 ROLE_IN_USE
     * 需求：9.5
     */
    @Transactional
    public void deleteRole(Long id) {
        int userCount = roleMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        // 先删除角色权限关联
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteById(id);
        log.info("删除角色成功: id={}", id);
    }

    /**
     * 为角色分配权限，同时清除该角色下所有用户的权限缓存并使 token 失效
     * 需求：9.6、6.5
     */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.insertRolePermissions(roleId, permissionIds);
        }
        // 清除该角色下所有用户的权限缓存，并使其 token 失效
        // 这样权限变更后用户下次请求会重新加载最新权限
        List<Long> userIds = roleMapper.findUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            userService.clearPermissionCache(userId);
            // 删除 Refresh Token，强制用户重新登录以获取最新权限的 token
            userService.invalidateRefreshToken(userId);
        }
        log.info("角色权限分配成功: roleId={}, permissionCount={}, affectedUsers={}",
                roleId, permissionIds == null ? 0 : permissionIds.size(), userIds.size());
    }
}
