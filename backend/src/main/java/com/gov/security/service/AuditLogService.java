package com.gov.security.service;

import com.gov.security.entity.AuditLog;
import com.gov.security.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审计日志服务
 * 需求：10.2、10.3、10.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 异步写入审计日志，不阻塞主业务流程
     * 需求：10.3
     */
    @Async("auditLogExecutor")
    public void asyncLog(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志写入失败: operationType={}, operatorId={}",
                    auditLog.getOperationType(), auditLog.getOperatorId(), e);
        }
    }

    /**
     * 分页查询审计日志
     * 需求：10.4
     */
    public Map<String, Object> findPage(Long operatorId, String type,
                                         String startTime, String endTime,
                                         int page, int size) {
        int offset = (page - 1) * size;
        List<AuditLog> list = auditLogMapper.findPage(operatorId, type, startTime, endTime, offset, size);
        Long total = auditLogMapper.countPage(operatorId, type, startTime, endTime);
        return Map.of("total", total, "list", list);
    }
}
