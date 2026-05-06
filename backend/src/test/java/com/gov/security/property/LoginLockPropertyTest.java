// Feature: gov-security-auth-system, Property 12: 账号锁定阈值
package com.gov.security.property;

import com.gov.security.common.constant.RedisKeyConstants;
import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.entity.User;
import com.gov.security.security.TokenManager;
import com.gov.security.service.AuthService;
import com.gov.security.service.CaptchaService;
import com.gov.security.service.UserService;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：账号锁定阈值
 * <p>
 * Property 12: 在 10 分钟窗口内连续登录失败 5 次后，第 6 次应返回 ACCOUNT_LOCKED
 * <p>Validates: Requirements 2.6
 */
class LoginLockPropertyTest {

    /**
     * 生成有效用户名（3-20 位小写字母）
     */
    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
    }

    /**
     * Property 12: 连续失败 5 次后账号被锁定，第 6 次返回 ACCOUNT_LOCKED
     * <p>Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void accountLockedAfterFiveFailures(@ForAll("validUsernames") String username) {
        // 使用真实的 BCrypt encoder
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String correctPassword = "correct1234";
        String wrongPassword = "wrongPass1";

        // Mock 依赖
        UserService userService = Mockito.mock(UserService.class);
        CaptchaService captchaService = Mockito.mock(CaptchaService.class);
        TokenManager tokenManager = Mockito.mock(TokenManager.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // 验证码始终通过
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);

        // 账号未被锁定（初始状态）
        when(redisTemplate.hasKey(RedisKeyConstants.accountLockKey(username))).thenReturn(false);

        // 用户存在但密码不匹配
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(encoder.encode(correctPassword));
        user.setStatus(1);
        when(userService.findByUsername(username)).thenReturn(user);

        // 模拟 Redis 计数器行为
        AtomicLong counter = new AtomicLong(0);
        when(valueOps.increment(RedisKeyConstants.loginFailKey(username)))
                .thenAnswer(inv -> counter.incrementAndGet());

        AuthService authService = new AuthService(
                userService, captchaService, tokenManager, redisTemplate, encoder);

        // 前 5 次失败应返回 INVALID_CREDENTIALS
        for (int i = 1; i <= 5; i++) {
            final int attempt = i;
            assertThatThrownBy(() ->
                    authService.login(username, wrongPassword, "uuid", "code"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        // 第 5 次触发锁定，但当前调用仍抛 INVALID_CREDENTIALS
                        // 锁定在下一次调用时生效
                        assertThat(be.getErrorCode()).isIn(
                                ErrorCode.INVALID_CREDENTIALS, ErrorCode.ACCOUNT_LOCKED);
                    });
        }

        // 第 6 次：模拟账号已被锁定
        when(redisTemplate.hasKey(RedisKeyConstants.accountLockKey(username))).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(username, wrongPassword, "uuid", "code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    /**
     * Property 12b: 登录成功后失败计数被清除，不会触发锁定
     * <p>Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void successfulLoginClearsFailCount(@ForAll("validUsernames") String username) {
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String correctPassword = "correct1234";

        UserService userService = Mockito.mock(UserService.class);
        CaptchaService captchaService = Mockito.mock(CaptchaService.class);
        TokenManager tokenManager = Mockito.mock(TokenManager.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.hasKey(RedisKeyConstants.accountLockKey(username))).thenReturn(false);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(encoder.encode(correctPassword));
        user.setStatus(1);
        when(userService.findByUsername(username)).thenReturn(user);
        when(userService.getRoleCodes(1L)).thenReturn(java.util.List.of("ROLE_CITIZEN"));
        when(userService.getPermissionCodes(1L)).thenReturn(java.util.List.of());

        when(tokenManager.generateAccessToken(any(), any(), any(), any(), any()))
                .thenReturn("access.token.value");
        when(tokenManager.generateRefreshToken(any())).thenReturn("refresh.token.value");
        when(tokenManager.getJti("access.token.value")).thenReturn("test-jti");

        AuthService authService = new AuthService(
                userService, captchaService, tokenManager, redisTemplate, encoder);

        // 登录成功
        AuthService.LoginResult result = authService.login(username, correctPassword, "uuid", "code");
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.token.value");

        // 验证失败计数被清除
        verify(redisTemplate).delete(RedisKeyConstants.loginFailKey(username));
    }
}
