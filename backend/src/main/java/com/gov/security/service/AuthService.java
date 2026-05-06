package com.gov.security.service;

import com.gov.security.common.annotation.Auditable;
import com.gov.security.common.constant.RedisKeyConstants;
import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.entity.User;
import com.gov.security.security.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证核心服务
 * 处理注册、登录、登出、Token 刷新、密码修改
 * 需求：1、2、3、4、5、13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAIL_COUNT = 5;
    private static final long FAIL_WINDOW_MINUTES = 10L;
    private static final long LOCK_MINUTES = 30L;

    private final UserService userService;
    private final CaptchaService captchaService;
    private final TokenManager tokenManager;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    // ---- 注册 ----

    public void register(String username, String rawPassword, String phone, String realName) {
        userService.register(username, rawPassword, phone, realName);
    }

    // ---- 登录 ----

    /**
     * 登录
     * 需求：2.3 ~ 2.9
     */
    @Auditable(operationType = "LOGIN")
    public LoginResult login(String username, String rawPassword, String captchaUuid, String captchaCode) {
        // 2.3 优先校验验证码
        if (!captchaService.verify(captchaUuid, captchaCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }

        // 2.6 检查账号是否被锁定
        String lockKey = RedisKeyConstants.accountLockKey(username);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 2.4 校验账号密码
        User user = userService.findByUsername(username);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            recordLoginFail(username);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2.9 检查账号禁用状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(RedisKeyConstants.loginFailKey(username));

        // 2.7 生成 Token
        List<String> roles = userService.getRoleCodes(user.getId());
        List<String> permissions = userService.getPermissionCodes(user.getId());
        String dataScope = resolveDataScope(roles);

        String accessToken = tokenManager.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions, dataScope);
        String refreshToken = tokenManager.generateRefreshToken(user.getId());

        // 2.8 写入 Redis
        String jti = tokenManager.getJti(accessToken);
        redisTemplate.opsForValue().set(
                RedisKeyConstants.accessTokenKey(user.getId(), jti),
                "1", 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(
                RedisKeyConstants.refreshTokenKey(user.getId()),
                refreshToken, 7, TimeUnit.DAYS);

        log.info("用户登录成功: username={}", username);
        return new LoginResult(accessToken, refreshToken,
                new UserInfo(user.getId(), user.getUsername(), user.getRealName(), roles, permissions));
    }

    /**
     * 记录登录失败，达到阈值则锁定账号
     * 需求：2.6
     */
    private void recordLoginFail(String username) {
        String failKey = RedisKeyConstants.loginFailKey(username);
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count == 1) {
            // 首次失败，设置 10 分钟窗口
            redisTemplate.expire(failKey, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= MAX_FAIL_COUNT) {
            // 锁定账号 30 分钟
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.accountLockKey(username),
                    "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("账号已被锁定: username={}", username);
        }
    }

    // ---- Token 刷新 ----

    /**
     * 刷新 Access Token（一次性 Refresh Token）
     * 需求：4.1 ~ 4.4
     */
    public String refresh(String refreshToken) {
        Long userId;
        try {
            userId = tokenManager.getUserId(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (userId == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 校验 Redis 中存储的 Refresh Token 是否一致
        String storedKey = RedisKeyConstants.refreshTokenKey(userId);
        String stored = redisTemplate.opsForValue().get(storedKey);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 4.4 一次性机制：立即删除旧 Refresh Token
        redisTemplate.delete(storedKey);

        // 生成新 Access Token
        User user = userService.findById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        List<String> roles = userService.getRoleCodes(userId);
        List<String> permissions = userService.getPermissionCodes(userId);
        String dataScope = resolveDataScope(roles);

        String newAccessToken = tokenManager.generateAccessToken(
                userId, user.getUsername(), roles, permissions, dataScope);
        String newRefreshToken = tokenManager.generateRefreshToken(userId);

        // 更新 Redis
        String jti = tokenManager.getJti(newAccessToken);
        redisTemplate.opsForValue().set(
                RedisKeyConstants.accessTokenKey(userId, jti),
                "1", 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(
                storedKey, newRefreshToken, 7, TimeUnit.DAYS);

        return newAccessToken;
    }

    // ---- 登出 ----

    /**
     * 登出：将 Access Token 加入黑名单，删除 Refresh Token
     * 需求：5.1 ~ 5.3
     */
    @Auditable(operationType = "LOGOUT")
    public void logout(String authHeader) {
        String token = extractBearerToken(authHeader);
        String jti = tokenManager.getJti(token);
        Long userId = tokenManager.getUserId(token);

        // 5.1 加入黑名单，TTL = Token 剩余有效期
        long expiry = tokenManager.getExpiration(token);
        long remaining = expiry - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.tokenBlacklistKey(jti),
                    "1", remaining, TimeUnit.MILLISECONDS);
        }

        // 5.2 删除 Refresh Token
        if (userId != null) {
            redisTemplate.delete(RedisKeyConstants.refreshTokenKey(userId));
        }
        log.info("用户登出成功: userId={}", userId);
    }

    // ---- 密码修改 ----

    /**
     * 修改密码
     * 需求：13.3、13.4、10.1
     */
    @Auditable(operationType = "PASSWORD_CHANGE")
    public void changePassword(String authHeader, String currentRaw, String newRaw) {
        String token = extractBearerToken(authHeader);
        Long userId = tokenManager.getUserId(token);
        userService.changePassword(userId, currentRaw, newRaw);

        // 使该用户所有在线 Token 失效（删除 Refresh Token，Access Token 靠黑名单）
        redisTemplate.delete(RedisKeyConstants.refreshTokenKey(userId));
        // 清除权限缓存
        redisTemplate.delete(RedisKeyConstants.userPermissionsKey(userId));
    }

    // ---- helpers ----

    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new BusinessException(ErrorCode.TOKEN_MISSING);
    }

    private String resolveDataScope(List<String> roles) {
        if (roles.contains("ROLE_SUPER_ADMIN")) return "ALL";
        if (roles.contains("ROLE_OFFICER")) return "DEPT";
        return "SELF";
    }

    // ---- Response DTOs ----

    public record LoginResult(String accessToken, String refreshToken, UserInfo user) {}

    public record UserInfo(Long id, String username, String realName,
                            List<String> roles, List<String> permissions) {}
}
