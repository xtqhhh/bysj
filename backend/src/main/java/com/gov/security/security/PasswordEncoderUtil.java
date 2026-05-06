package com.gov.security.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类（静态方法，非 Spring Bean）
 *
 * <p><b>安全规范：</b>
 * <ul>
 *   <li>严禁将原始密码（rawPassword）记录到任何日志（Logger、System.out 等）中。</li>
 *   <li>严禁将原始密码包含在任何 HTTP 响应体中返回给客户端。</li>
 *   <li>数据库中只允许存储经本工具类加密后的密码哈希值，严禁明文存储。</li>
 *   <li>所有密码字段在序列化时应标注 {@code @JsonIgnore} 以防止意外泄露。</li>
 * </ul>
 */
public final class PasswordEncoderUtil {

    /** BCrypt cost factor，与 SecurityConfig 保持一致 */
    private static final int STRENGTH = 10;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(STRENGTH);

    private PasswordEncoderUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 对原始密码进行 BCrypt 加密。
     *
     * <p><b>警告：</b>调用方严禁将 {@code rawPassword} 记录到日志中。
     *
     * @param rawPassword 原始明文密码（不得为 null）
     * @return BCrypt 哈希字符串
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证原始密码与已加密密码是否匹配。
     *
     * <p><b>警告：</b>调用方严禁将 {@code rawPassword} 记录到日志中。
     *
     * @param rawPassword     原始明文密码
     * @param encodedPassword 数据库中存储的 BCrypt 哈希值
     * @return 匹配返回 {@code true}，否则返回 {@code false}
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
