package com.gov.security.mapper;

import com.gov.security.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM role WHERE role_code = #{roleCode} LIMIT 1")
    Role findByRoleCode(String roleCode);

    @Select("SELECT * FROM role WHERE id = #{id} LIMIT 1")
    Role findById(Long id);

    @Select("SELECT * FROM role ORDER BY id")
    List<Role> findAll();

    @Insert("INSERT INTO role(role_name, role_code, data_scope, create_time) " +
            "VALUES(#{roleName}, #{roleCode}, #{dataScope}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);

    @Update("UPDATE role SET role_name=#{roleName}, role_code=#{roleCode}, data_scope=#{dataScope} WHERE id=#{id}")
    int update(Role role);

    @Delete("DELETE FROM role WHERE id = #{id}")
    int deleteById(Long id);

    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int assignRoleToUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 检查角色是否有关联用户 */
    @Select("SELECT COUNT(*) FROM user_role WHERE role_id = #{roleId}")
    int countUsersByRoleId(Long roleId);

    /** 删除角色的所有权限关联 */
    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(Long roleId);

    /** 批量插入角色权限关联 */
    @Insert("<script>" +
            "INSERT IGNORE INTO role_permission(role_id, permission_id) VALUES " +
            "<foreach collection='permissionIds' item='pid' separator=','>" +
            "(#{roleId}, #{pid})" +
            "</foreach>" +
            "</script>")
    int insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /** 查询角色下所有用户 ID（用于清除权限缓存） */
    @Select("SELECT user_id FROM user_role WHERE role_id = #{roleId}")
    List<Long> findUserIdsByRoleId(Long roleId);
}
