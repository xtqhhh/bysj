package com.gov.security.mapper;

import com.gov.security.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 * 需求：8.1
 */
@Mapper
public interface MenuMapper {

    /**
     * 查询某用户有权访问的所有菜单（目录+菜单+按钮），通过角色-权限关联获取
     * 仅返回状态为启用（status=1）的菜单
     */
    @Select("SELECT DISTINCT m.* FROM menu m " +
            "INNER JOIN permission p ON m.permission_code = p.permission_code " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 " +
            "ORDER BY m.parent_id, m.order_num, m.id")
    List<Menu> findByUserId(Long userId);

    /**
     * 查询所有启用状态的菜单（用于超级管理员）
     */
    @Select("SELECT * FROM menu WHERE status = 1 ORDER BY parent_id, order_num, id")
    List<Menu> findAll();
}
