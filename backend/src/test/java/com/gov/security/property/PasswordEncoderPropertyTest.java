// Feature: gov-security-auth-system, Property 1: 密码加密不可逆且可验证
package com.gov.security.property;

import com.gov.security.security.PasswordEncoderUtil;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;

/**
 * 属性测试：BCrypt 密码加密不可逆且可验证
 *
 * <p><b>Validates: Requirements 1.2</b>
 */
class PasswordEncoderPropertyTest {

    /**
     * 提供有效密码：长度 8-50，包含字母和数字
     */
    @Provide
    Arbitrary<String> validPasswords() {
        // 生成 4-21 位小写字母前缀 + "1234" 后缀，总长度 8-25
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(4)
                .ofMaxLength(46)
                .map(s -> s + "1234");
    }

    /**
     * Property 1: BCrypt 加密不可逆且可验证
     * - encode(password) 的结果不等于原始密码（不可逆）
     * - matches(password, encode(password)) 返回 true（可验证）
     *
     * <p><b>Validates: Requirements 1.2</b>
     */
    @Property(tries = 100)
    void passwordEncodingIsIrreversibleAndVerifiable(@ForAll("validPasswords") String password) {
        String encoded = PasswordEncoderUtil.encode(password);

        // 加密结果不等于原始密码（不可逆）
        Assertions.assertThat(encoded).isNotEqualTo(password);

        // 原始密码与加密结果可以匹配（可验证）
        Assertions.assertThat(PasswordEncoderUtil.matches(password, encoded)).isTrue();
    }
}
