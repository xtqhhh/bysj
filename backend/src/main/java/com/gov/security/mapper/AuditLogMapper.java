package com.gov.security.mapper;

import com.gov.security.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志 Mapper
 * 需求：10.2、10.4
 */
@Mapper
public interface AuditLogMapper {

    @Insert("INSERT INTO audit_log(operator_id, operator_name, operation_type, request_ip, " +
            "operation_time, result, before_data, after_data) " +
            "VALUES(#{operatorId}, #{operatorName}, #{operationType}, #{requestIp}, " +
            "#{operationTime}, #{result}, #{beforeData}, #{afterData})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog log);

    /**
     * 分页查询审计日志（使用 XML mapper）
     */
    List<AuditLog> findPage(@Param("operatorId") Long operatorId,
                            @Param("type") String type,
                            @Param("startTime") String startTime,
                            @Param("endTime") String endTime,
                            @Param("offset") int offset,
                            @Param("size") int size);

    /**
     * 统计审计日志总数（使用 XML mapper）
     */
    Long countPage(@Param("operatorId") Long operatorId,
                   @Param("type") String type,
                   @Param("startTime") String startTime,
                   @Param("endTime") String endTime);
}
