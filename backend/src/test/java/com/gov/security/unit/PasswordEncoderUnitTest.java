package com.gov.security.unit;

import com.gov.security.security.PasswordEncoderUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试：BCrypt 密码加密与验证
 * 需求：1.2、13.1
 */
class PasswordEncoderUnitTest {

    @Test
    void encodedPasswordIsNotEqualToRawPassword() {
        String raw = "Password123";
        String encoded = PasswordEncoderUtil.encode(raw);
        assertThat(encoded).isNotEqualTo(raw);
    }

    @Test
    void encodedPasswordStartsWithBcryptPrefix() {
        String encoded = PasswordEncoderUtil.encode("Password123");
        // BCrypt 哈希以 $2a$ 或 $2b$ 开头
        assertThat(encoded).matches("\\$2[ab]\\$.*");
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String raw = "MySecret99";
        String encoded = PasswordEncoderUtil.encode(raw);
        assertThat(PasswordEncoderUtil.matches(raw, encoded)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String raw = "MySecret99";
        String encoded = PasswordEncoderUtil.encode(raw);
        assertThat(PasswordEncoderUtil.matches("WrongPass1", encoded)).isFalse();
    }

    @Test
    void samePasswordProducesDifferentHashes() {
        // BCrypt 每次加盐，相同密码产生不同哈希
        String raw = "SamePass1";
        String hash1 = PasswordEncoderUtil.encode(raw);
        String hash2 = PasswordEncoderUtil.encode(raw);
        assertThat(hash1).isNotEqualTo(hash2);
        // 但两者都能验证通过
        assertThat(PasswordEncoderUtil.matches(raw, hash1)).isTrue();
        assertThat(PasswordEncoderUtil.matches(raw, hash2)).isTrue();
    }

    @Test
    void matchesReturnsFalseForEmptyPassword() {
        String encoded = PasswordEncoderUtil.encode("Password123");
        assertThat(PasswordEncoderUtil.matches("", encoded)).isFalse();
    }
}
