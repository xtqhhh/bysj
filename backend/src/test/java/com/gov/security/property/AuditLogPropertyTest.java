package com.gov.security.property;

// Feature: gov-security-auth-system, Property 9

import com.gov.security.entity.AuditLog;
import net.jqwik.api.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 属性 9：审计日志字段完整性
 * 验证需求：10.2
 *
 * 对于任意 AuditLog 对象，若其必填字段（operatorId、operationType、
 * operationTime、requestIp、result）均已设置，则这些字段均不为 null。
 */
class AuditLogPropertyTest {

    // Feature: gov-security-auth-system, Property 9
    @Property(tries = 100)
    void auditLogFieldCompleteness(
            @ForAll("operatorIds") Long operatorId,
            @ForAll("operationTypes") String operationType,
            @ForAll("ips") String requestIp,
            @ForAll("results") String result) {

        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setOperatorName("user_" + operatorId);
        log.setOperationType(operationType);
        log.setRequestIp(requestIp);
        log.setOperationTime(LocalDateTime.now());
        log.setResult(result);

        // 验证所有必填字段均不为 null（需求 10.2）
        assertThat(log.getOperatorId()).isNotNull();
        assertThat(log.getOperationType()).isNotNull().isNotBlank();
        assertThat(log.getOperationTime()).isNotNull();
        assertThat(log.getRequestIp()).isNotNull().isNotBlank();
        assertThat(log.getResult()).isNotNull().isNotBlank();
    }

    @Provide
    Arbitrary<Long> operatorIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> operationTypes() {
        return Arbitraries.of(
                "LOGIN", "LOGOUT", "LOGIN_FAIL",
                "PASSWORD_CHANGE", "ROLE_CHANGE",
                "PERMISSION_CHANGE", "USER_DISABLE", "USER_ENABLE"
        );
    }

    @Provide
    Arbitrary<String> ips() {
        return Arbitraries.of(
                "127.0.0.1", "192.168.1.1", "10.0.0.1",
                "172.16.0.1", "203.0.113.5"
        );
    }

    @Provide
    Arbitrary<String> results() {
        return Arbitraries.of("SUCCESS", "FAIL");
    }
}
