package com.gov.security.common.response;

import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
public enum ErrorCode {

    // 注册相关
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "用户名已存在"),
    INVALID_PHONE_FORMAT("INVALID_PHONE_FORMAT", "手机号格式错误"),
    WEAK_PASSWORD("WEAK_PASSWORD", "密码强度不足，密码长度不少于8位且需包含字母与数字的组合"),

    // 登录相关
    CAPTCHA_INVALID("CAPTCHA_INVALID", "验证码错误或已过期"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账号已被锁定，请30分钟后重试"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已被禁用，请联系管理员"),

    // Token 相关
    TOKEN_MISSING("TOKEN_MISSING", "Token缺失，请先登录"),
    TOKEN_INVALID("TOKEN_INVALID", "Token签名无效"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token已过期，请刷新"),
    TOKEN_REVOKED("TOKEN_REVOKED", "Token已被吊销"),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "Refresh Token无效或已过期，请重新登录"),

    // 权限相关
    ACCESS_DENIED("ACCESS_DENIED", "权限不足，无法访问该资源"),

    // 密码相关
    CURRENT_PASSWORD_WRONG("CURRENT_PASSWORD_WRONG", "当前密码错误"),

    // 角色相关
    ROLE_IN_USE("ROLE_IN_USE", "该角色仍有关联用户，无法删除"),

    // 通用错误
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误，请稍后重试"),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
