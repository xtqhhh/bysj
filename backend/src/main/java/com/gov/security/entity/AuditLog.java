package com.gov.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 需求：10.1、10.2
 */
@Data
public class AuditLog {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operationType;
    private String requestIp;
    private LocalDateTime operationTime;
    private String result;
    private String beforeData;
    private String afterData;
}
