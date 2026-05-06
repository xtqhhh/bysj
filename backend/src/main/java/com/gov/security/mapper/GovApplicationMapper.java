package com.gov.security.mapper;

import com.gov.security.entity.GovApplication;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GovApplicationMapper {

    @Insert("INSERT INTO gov_application(apply_no, user_id, user_name, real_name, type, title, description, status) " +
            "VALUES(#{applyNo}, #{userId}, #{userName}, #{realName}, #{type}, #{title}, #{description}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GovApplication app);

    @Select("SELECT * FROM gov_application WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}")
    List<GovApplication> findByUserId(@Param("userId") Long userId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM gov_application WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    @Select("SELECT * FROM gov_application WHERE id = #{id}")
    GovApplication findById(Long id);

    @Select("SELECT * FROM gov_application WHERE apply_no = #{applyNo}")
    GovApplication findByApplyNo(String applyNo);

    /** 管理员查询（支持按状态、类型过滤） */
    @Select("<script>SELECT * FROM gov_application " +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<GovApplication> findPage(@Param("status") Integer status, @Param("type") String type,
                                   @Param("size") int size, @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM gov_application " +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "</where></script>")
    long countPage(@Param("status") Integer status, @Param("type") String type);

    /** 更新状态和备注 */
    @Update("UPDATE gov_application SET status = #{status}, remark = #{remark} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") int status, @Param("remark") String remark);
}
