package com.gov.security.common.constant;

/**
 * Redis Key 命名规范常量类
 *
 * <p>命名规范：{前缀}:{业务标识}</p>
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    /** 图形验证码：captcha:{uuid} */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /** Access Token 存活标记：access_token:{userId}:{jti} */
    public static final String ACCESS_TOKEN_PREFIX = "access_token:";

    /** Refresh Token：refresh_token:{userId} */
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /** Token 黑名单：token_blacklist:{jti} */
    public static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";

    /** 登录失败计数：login_fail:{username} */
    public static final String LOGIN_FAIL_PREFIX = "login_fail:";

    /** 账号锁定标记：account_lock:{username} */
    public static final String ACCOUNT_LOCK_PREFIX = "account_lock:";

    /** 用户权限缓存：user_permissions:{userId} */
    public static final String USER_PERMISSIONS_PREFIX = "user_permissions:";

    // ---- 便捷构建方法 ----

    public static String captchaKey(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }

    public static String accessTokenKey(Long userId, String jti) {
        return ACCESS_TOKEN_PREFIX + userId + ":" + jti;
    }

    public static String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String tokenBlacklistKey(String jti) {
        return TOKEN_BLACKLIST_PREFIX + jti;
    }

    public static String loginFailKey(String username) {
        return LOGIN_FAIL_PREFIX + username;
    }

    public static String accountLockKey(String username) {
        return ACCOUNT_LOCK_PREFIX + username;
    }

    public static String userPermissionsKey(Long userId) {
        return USER_PERMISSIONS_PREFIX + userId;
    }
}
