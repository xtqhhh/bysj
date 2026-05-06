package com.gov.security.mapper;

import com.gov.security.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO message(user_id, user_name, real_name, content, audit_status, status) " +
            "VALUES(#{userId}, #{userName}, #{realName}, #{content}, 0, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Message message);

    @Select("SELECT * FROM message WHERE user_id = #{userId} AND status = 1 ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}")
    List<Message> findByUserId(@Param("userId") Long userId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM message WHERE user_id = #{userId} AND status = 1")
    long countByUserId(Long userId);

    /** 公开查询：仅返回审核通过的留言，按时间倒序 */
    @Select("SELECT * FROM message WHERE audit_status = 1 AND status = 1 ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}")
    List<Message> findApproved(@Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM message WHERE audit_status = 1 AND status = 1")
    long countApproved();

    /** 管理员查询：支持按审核状态过滤，deleted=true 时只查已删除 */
    @Select("<script>SELECT * FROM message " +
            "<where>" +
            "<choose>" +
            "<when test='deleted == true'>AND status = 0</when>" +
            "<otherwise>AND status = 1</otherwise>" +
            "</choose>" +
            "<if test='auditStatus != null'>AND audit_status = #{auditStatus}</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Message> findPage(@Param("auditStatus") Integer auditStatus,
                           @Param("deleted") boolean deleted,
                           @Param("size") int size, @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM message " +
            "<where>" +
            "<choose>" +
            "<when test='deleted == true'>AND status = 0</when>" +
            "<otherwise>AND status = 1</otherwise>" +
            "</choose>" +
            "<if test='auditStatus != null'>AND audit_status = #{auditStatus}</if>" +
            "</where></script>")
    long countPage(@Param("auditStatus") Integer auditStatus, @Param("deleted") boolean deleted);

    @Select("SELECT * FROM message WHERE id = #{id}")
    Message findById(Long id);

    /** 审核：更新审核状态 */
    @Update("UPDATE message SET audit_status = #{auditStatus} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("auditStatus") int auditStatus);

    /** 回复 */
    @Update("UPDATE message SET reply = #{reply}, reply_time = NOW() WHERE id = #{id}")
    void reply(@Param("id") Long id, @Param("reply") String reply);

    /** 删除（逻辑删除） */
    @Update("UPDATE message SET status = 0 WHERE id = #{id}")
    void delete(Long id);
}
