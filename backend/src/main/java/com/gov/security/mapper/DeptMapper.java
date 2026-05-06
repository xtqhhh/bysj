package com.gov.security.mapper;

import com.gov.security.entity.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 部门 Mapper
 * 需求：7.1、7.2
 */
@Mapper
public interface DeptMapper {

    @Select("SELECT * FROM dept WHERE id = #{id} LIMIT 1")
    Dept findById(Long id);

    @Select("SELECT * FROM dept WHERE status = 1 ORDER BY order_num, id")
    List<Dept> findAllEnabled();

    @Select("SELECT * FROM dept WHERE parent_id = #{parentId} AND status = 1 ORDER BY order_num, id")
    List<Dept> findByParentId(Long parentId);

    @Insert("INSERT INTO dept(dept_name, parent_id, order_num, status, create_time) " +
            "VALUES(#{deptName}, #{parentId}, #{orderNum}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dept dept);

    @Update("UPDATE dept SET dept_name = #{deptName}, parent_id = #{parentId}, " +
            "order_num = #{orderNum}, status = #{status} WHERE id = #{id}")
    int update(Dept dept);

    @Update("UPDATE dept SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
