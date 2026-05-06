package com.gov.security.unit;

import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.entity.User;
import com.gov.security.service.AuthService;
import com.gov.security.service.CaptchaService;
import com.gov.security.service.UserService;
import com.gov.security.security.TokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 单元测试：登录完整流程（含验证码优先校验）
 * 需求：2.3、2.4、2.6、2.9
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceUnitTest {

    @Mock
    private UserService userService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthService authService;

    // ---- 验证码优先校验 ----

    @Test
    void loginFailsWhenCaptchaIsInvalid() {
        // 验证码校验失败
        when(captchaService.verify("uuid1", "wrongCode")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user", "pass", "uuid1", "wrongCode"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CAPTCHA_INVALID));

        // 验证码失败后不应查询用户
        verifyNoInteractions(userService);
    }

    @Test
    void loginFailsWhenCaptchaIsExpired() {
        // uuid 不存在（已过期）
        when(captchaService.verify("expiredUuid", "anyCode")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user", "pass", "expiredUuid", "anyCode"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CAPTCHA_INVALID));
    }

    // ---- 账号锁定 ----

    @Test
    void loginFailsWhenAccountIsLocked() {
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        // 账号锁定 key 存在
        when(redisTemplate.hasKey(contains("account_lock:"))).thenReturn(true);

        assertThatThrownBy(() -> authService.login("lockedUser", "pass", "uuid", "code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    // ---- 凭证校验 ----

    @Test
    void loginFailsWhenUserNotFound() {
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.findByUsername("unknown")).thenReturn(null);
        // 记录失败次数
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login("unknown", "pass", "uuid", "code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void loginFailsWhenPasswordIsWrong() {
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = buildUser(1L, "alice", "encodedPass", 1);
        when(userService.findByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login("alice", "wrongPass", "uuid", "code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    // ---- 账号禁用 ----

    @Test
    void loginFailsWhenAccountIsDisabled() {
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = buildUser(2L, "bob", "encodedPass", 0); // status=0 禁用
        when(userService.findByUsername("bob")).thenReturn(user);
        when(passwordEncoder.matches("pass123", "encodedPass")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("bob", "pass123", "uuid", "code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    // ---- 登录成功 ----

    @Test
    void loginSucceedsWithValidCredentials() {
        when(captchaService.verify("uuid", "code")).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = buildUser(3L, "charlie", "encodedPass", 1);
        when(userService.findByUsername("charlie")).thenReturn(user);
        when(passwordEncoder.matches("pass123", "encodedPass")).thenReturn(true);
        when(userService.getRoleCodes(3L)).thenReturn(List.of("ROLE_CITIZEN"));
        when(userService.getPermissionCodes(3L)).thenReturn(List.of());
        when(tokenManager.generateAccessToken(anyLong(), anyString(), anyList(), anyList(), anyString()))
                .thenReturn("access.token.value");
        when(tokenManager.generateRefreshToken(anyLong())).thenReturn("refresh.token.value");
        when(tokenManager.getJti("access.token.value")).thenReturn("jti-123");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthService.LoginResult result = authService.login("charlie", "pass123", "uuid", "code");

        assertThat(result.accessToken()).isEqualTo("access.token.value");
        assertThat(result.refreshToken()).isEqualTo("refresh.token.value");
        assertThat(result.user().username()).isEqualTo("charlie");
        assertThat(result.user().roles()).containsExactly("ROLE_CITIZEN");
    }

    @Test
    void loginClearsFailCountOnSuccess() {
        when(captchaService.verify("uuid", "code")).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = buildUser(4L, "dave", "encodedPass", 1);
        when(userService.findByUsername("dave")).thenReturn(user);
        when(passwordEncoder.matches("pass123", "encodedPass")).thenReturn(true);
        when(userService.getRoleCodes(4L)).thenReturn(List.of("ROLE_CITIZEN"));
        when(userService.getPermissionCodes(4L)).thenReturn(List.of());
        when(tokenManager.generateAccessToken(anyLong(), anyString(), anyList(), anyList(), anyString()))
                .thenReturn("access.token");
        when(tokenManager.generateRefreshToken(anyLong())).thenReturn("refresh.token");
        when(tokenManager.getJti("access.token")).thenReturn("jti-456");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.login("dave", "pass123", "uuid", "code");

        // 登录成功后应删除失败计数 key
        verify(redisTemplate).delete(contains("login_fail:"));
    }

    // ---- helper ----

    private User buildUser(Long id, String username, String encodedPassword, int status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRealName(username);
        user.setStatus(status);
        return user;
    }
}
