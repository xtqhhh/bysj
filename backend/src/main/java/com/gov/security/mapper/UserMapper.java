package com.gov.security.mapper;

import com.gov.security.controller.dto.UserPageVO;
import com.gov.security.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE id = #{id} LIMIT 1")
    User findById(Long id);

    @Insert("INSERT INTO user(username, password, phone, real_name, status, dept_id, create_time) " +
            "VALUES(#{username}, #{password}, #{phone}, #{realName}, 1, #{deptId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE user SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 查询用户的角色编码列表 */
    @Select("SELECT r.role_code FROM role r " +
            "INNER JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findRoleCodesByUserId(Long userId);

    /** 查询用户的权限编码列表 */
    @Select("SELECT DISTINCT p.permission_code FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findPermissionCodesByUserId(Long userId);

    // ---- 管理员用户管理 ----

    /**
     * 分页查询用户列表（含部门名称），动态条件
     * 需求：9.1
     */
    List<UserPageVO> findPage(@Param("username") String username,
                              @Param("role") String role,
                              @Param("status") Integer status,
                              @Param("size") int size,
                              @Param("offset") long offset);

    /**
     * 统计分页查询总数
     * 需求：9.1
     */
    long countPage(@Param("username") String username,
                   @Param("role") String role,
                   @Param("status") Integer status);

    /**
     * 查询用户的角色编码列表（用于 VO 填充）
     * 需求：9.1
     */
    @Select("SELECT r.role_code FROM role r " +
            "INNER JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findRolesByUserId(@Param("userId") Long userId);

    /**
     * 删除用户所有角色关联
     * 需求：9.2
     */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 批量插入用户角色关联
     * 需求：9.2
     */
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
