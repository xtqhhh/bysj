package com.gov.security.common.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解，标注需要记录审计日志的方法
 * 需求：10.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 操作类型，如 LOGIN、LOGOUT、LOGIN_FAIL、PASSWORD_CHANGE、
     * ROLE_CHANGE、PERMISSION_CHANGE、USER_DISABLE、USER_ENABLE
     */
    String operationType();
}
