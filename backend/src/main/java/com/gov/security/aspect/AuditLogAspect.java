package com.gov.security.aspect;

import com.gov.security.common.annotation.Auditable;
import com.gov.security.entity.AuditLog;
import com.gov.security.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志 AOP 切面
 * 拦截 @Auditable 注解的方法，记录操作日志
 * 需求：10.1、10.2、10.3
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String operationType = auditable.operationType();
        String requestIp = getClientIp();
        Long operatorId = null;
        String operatorName = null;

        // 对于登录操作，从方法参数中获取用户名
        if ("LOGIN".equals(operationType) || "LOGIN_FAIL".equals(operationType)) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && args[0] instanceof String) {
                operatorName = (String) args[0];
                operatorId = 0L; // 登录时尚未获取 userId，使用 0 占位
            }
        } else {
            // 其他操作从 SecurityContextHolder 获取操作人信息
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails userDetails) {
                operatorName = userDetails.getUsername();
            }
            if (auth != null && auth.getDetails() instanceof Long id) {
                operatorId = id;
            }
            // 尝试从 principal 中获取 userId（TokenManager 存储的方式）
            if (operatorId == null && auth != null) {
                try {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof org.springframework.security.core.userdetails.User u) {
                        operatorName = u.getUsername();
                    }
                } catch (Exception ignored) {
                    // ignore
                }
                // 从 credentials 中尝试获取
                if (auth.getCredentials() instanceof Long id) {
                    operatorId = id;
                }
            }
            if (operatorId == null) {
                operatorId = 0L;
            }
            if (operatorName == null) {
                operatorName = "unknown";
            }
        }

        boolean success = true;
        Throwable thrown = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            thrown = t;
        }

        // 对于登录操作，成功后从返回值中获取真实 userId
        String finalOperationType = operationType;
        if ("LOGIN".equals(operationType) && success && result != null) {
            try {
                // LoginResult record: accessToken, refreshToken, user(id, username, ...)
                Object userInfo = result.getClass().getMethod("user").invoke(result);
                if (userInfo != null) {
                    operatorId = (Long) userInfo.getClass().getMethod("id").invoke(userInfo);
                    operatorName = (String) userInfo.getClass().getMethod("username").invoke(userInfo);
                }
            } catch (Exception e) {
                log.debug("无法从登录结果中提取用户信息", e);
            }
        } else if ("LOGIN".equals(operationType) && !success) {
            finalOperationType = "LOGIN_FAIL";
        }

        // 构建审计日志
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(operatorId != null ? operatorId : 0L);
        auditLog.setOperatorName(operatorName != null ? operatorName : "unknown");
        auditLog.setOperationType(finalOperationType);
        auditLog.setRequestIp(requestIp);
        auditLog.setOperationTime(LocalDateTime.now());
        auditLog.setResult(success ? "SUCCESS" : "FAIL");

        // 异步写入，不阻塞主流程
        auditLogService.asyncLog(auditLog);

        if (thrown != null) {
            throw thrown;
        }
        return result;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能包含多个 IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
