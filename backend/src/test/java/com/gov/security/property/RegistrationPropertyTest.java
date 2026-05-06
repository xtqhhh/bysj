// Feature: gov-security-auth-system, Property 2: 弱密码被拒绝
// Feature: gov-security-auth-system, Property 3: 注册成功后默认角色为 ROLE_CITIZEN
package com.gov.security.property;

import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.entity.Role;
import com.gov.security.entity.User;
import com.gov.security.mapper.RoleMapper;
import com.gov.security.mapper.UserMapper;
import com.gov.security.service.UserService;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：注册相关属性
 * <p>
 * Property 2: 弱密码被拒绝 — 验证需求 1.5
 * Property 3: 注册成功后默认角色为 ROLE_CITIZEN — 验证需求 1.6
 */
class RegistrationPropertyTest {

    // ---- Property 2: 弱密码被拒绝 ----

    /**
     * 生成弱密码：纯字母（无数字）
     */
    @Provide
    Arbitrary<String> lettersOnlyPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(8)
                .ofMaxLength(20);
    }

    /**
     * 生成弱密码：纯数字（无字母）
     */
    @Provide
    Arbitrary<String> digitsOnlyPasswords() {
        return Arbitraries.strings()
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(20);
    }

    /**
     * 生成弱密码：长度不足 8 位（含字母和数字但太短）
     */
    @Provide
    Arbitrary<String> tooShortPasswords() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz0123456789")
                .ofMinLength(1)
                .ofMaxLength(7);
    }

    /**
     * Property 2a: 纯字母密码被拒绝
     * <p>Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void lettersOnlyPasswordIsRejected(@ForAll("lettersOnlyPasswords") String password) {
        assertThat(UserService.isStrongPassword(password))
                .as("纯字母密码应被拒绝: %s", password)
                .isFalse();
    }

    /**
     * Property 2b: 纯数字密码被拒绝
     * <p>Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void digitsOnlyPasswordIsRejected(@ForAll("digitsOnlyPasswords") String password) {
        assertThat(UserService.isStrongPassword(password))
                .as("纯数字密码应被拒绝: %s", password)
                .isFalse();
    }

    /**
     * Property 2c: 长度不足 8 位的密码被拒绝
     * <p>Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void tooShortPasswordIsRejected(@ForAll("tooShortPasswords") String password) {
        assertThat(UserService.isStrongPassword(password))
                .as("长度不足8位的密码应被拒绝: %s", password)
                .isFalse();
    }

    /**
     * Property 2d: 弱密码注册时抛出 WEAK_PASSWORD 异常
     * <p>Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void weakPasswordThrowsExceptionOnRegister(@ForAll("lettersOnlyPasswords") String weakPassword) {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);

        when(userMapper.findByUsername(anyString())).thenReturn(null);

        UserService service = new UserService(userMapper, roleMapper, encoder, redisTemplate);

        assertThatThrownBy(() -> service.register("testuser", weakPassword, "13800138000", "测试用户"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.WEAK_PASSWORD));
    }

    // ---- Property 3: 注册成功后默认角色为 ROLE_CITIZEN ----

    /**
     * 生成有效密码：含字母和数字，长度 ≥ 8
     */
    @Provide
    Arbitrary<String> strongPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(4)
                .ofMaxLength(16)
                .map(s -> s + "1234");
    }

    /**
     * 生成有效用户名
     */
    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
    }

    /**
     * Property 3: 注册成功后，用户被分配 ROLE_CITIZEN 角色
     * <p>Validates: Requirements 1.6
     */
    @Property(tries = 100)
    void registeredUserHasRoleCitizen(
            @ForAll("validUsernames") String username,
            @ForAll("strongPasswords") String password) {

        UserMapper userMapper = Mockito.mock(UserMapper.class);
        RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);

        // 用户名不存在
        when(userMapper.findByUsername(username)).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });

        Role citizenRole = new Role();
        citizenRole.setId(1L);
        citizenRole.setRoleCode("ROLE_CITIZEN");
        when(roleMapper.findByRoleCode("ROLE_CITIZEN")).thenReturn(citizenRole);

        // 注册后查询角色
        when(userMapper.findRoleCodesByUserId(1L)).thenReturn(List.of("ROLE_CITIZEN"));

        UserService service = new UserService(userMapper, roleMapper, encoder, Mockito.mock(StringRedisTemplate.class));
        service.register(username, password, "13800138000", "测试用户");

        // 验证 assignRoleToUser 被调用，且传入了 ROLE_CITIZEN 的 roleId
        verify(roleMapper).assignRoleToUser(eq(1L), eq(1L));

        List<String> roles = userMapper.findRoleCodesByUserId(1L);
        assertThat(roles).containsExactly("ROLE_CITIZEN");
    }
}
