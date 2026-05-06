package com.gov.security.mapper;

import com.gov.security.entity.Permission;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 权限 Mapper
 * 需求：6.2
 */
@Mapper
public interface PermissionMapper {

    @Select("SELECT * FROM permission WHERE id = #{id} LIMIT 1")
    Permission findById(Long id);

    @Select("SELECT * FROM permission WHERE permission_code = #{permissionCode} LIMIT 1")
    Permission findByPermissionCode(String permissionCode);

    @Select("SELECT * FROM permission ORDER BY parent_id, id")
    List<Permission> findAll();

    /** 查询某角色拥有的所有权限 */
    @Select("SELECT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<Permission> findByRoleId(Long roleId);

    /** 查询某用户拥有的所有权限（跨角色去重） */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> findByUserId(Long userId);

    @Insert("INSERT INTO permission(permission_name, permission_code, type, parent_id, create_time) " +
            "VALUES(#{permissionName}, #{permissionCode}, #{type}, #{parentId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Permission permission);

    @Delete("DELETE FROM permission WHERE id = #{id}")
    int deleteById(Long id);

    /** 为角色分配权限 */
    @Insert("INSERT IGNORE INTO role_permission(role_id, permission_id) VALUES(#{roleId}, #{permissionId})")
    int assignPermissionToRole(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /** 删除角色的所有权限 */
    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deletePermissionsByRoleId(Long roleId);
}
