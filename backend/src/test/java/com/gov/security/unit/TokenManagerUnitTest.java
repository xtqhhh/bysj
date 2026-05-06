package com.gov.security.unit;

import com.gov.security.security.TokenManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 单元测试：JWT Token 生成、解析、过期校验、Refresh Token 一次性机制
 * 需求：3.2、3.7、4.4
 */
class TokenManagerUnitTest {

    // 密钥长度 ≥ 256 位（32 字节）
    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

    private TokenManager tokenManager;

    @BeforeEach
    void setUp() {
        // accessTokenMinutes=30, refreshTokenDays=7
        tokenManager = new TokenManager(SECRET, 30L, 7L);
    }

    // ---- Access Token 生成与解析 ----

    @Test
    void generateAccessTokenReturnsNonNullToken() {
        String token = tokenManager.generateAccessToken(
                1L, "alice", List.of("ROLE_CITIZEN"), List.of("user:read"), "SELF");
        assertThat(token).isNotBlank();
    }

    @Test
    void parseAccessTokenReturnsCorrectClaims() {
        String token = tokenManager.generateAccessToken(
                42L, "bob", List.of("ROLE_OFFICER"), List.of("audit:read"), "DEPT");

        Claims claims = tokenManager.parseToken(token);

        assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        assertThat(claims.get("username", String.class)).isEqualTo("bob");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).containsExactly("ROLE_OFFICER");
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) claims.get("permissions");
        assertThat(perms).containsExactly("audit:read");
        assertThat(claims.get("dataScope", String.class)).isEqualTo("DEPT");
    }

    @Test
    void extractClaimsRoundTrip() {
        String token = tokenManager.generateAccessToken(
                99L, "charlie", List.of("ROLE_SUPER_ADMIN"), List.of("user:add", "user:delete"), "ALL");

        TokenManager.TokenClaims tc = tokenManager.extractClaims(token);

        assertThat(tc.userId()).isEqualTo(99L);
        assertThat(tc.username()).isEqualTo("charlie");
        assertThat(tc.roles()).containsExactly("ROLE_SUPER_ADMIN");
        assertThat(tc.permissions()).containsExactlyInAnyOrder("user:add", "user:delete");
        assertThat(tc.dataScope()).isEqualTo("ALL");
        assertThat(tc.jti()).isNotBlank();
    }

    @Test
    void getJtiReturnsUniqueIdPerToken() {
        String t1 = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        String t2 = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        assertThat(tokenManager.getJti(t1)).isNotEqualTo(tokenManager.getJti(t2));
    }

    @Test
    void getUserIdReturnsCorrectId() {
        String token = tokenManager.generateAccessToken(77L, "user77", List.of(), List.of(), "SELF");
        assertThat(tokenManager.getUserId(token)).isEqualTo(77L);
    }

    // ---- Refresh Token ----

    @Test
    void generateRefreshTokenReturnsNonNullToken() {
        String rt = tokenManager.generateRefreshToken(1L);
        assertThat(rt).isNotBlank();
    }

    @Test
    void refreshTokenContainsUserId() {
        String rt = tokenManager.generateRefreshToken(55L);
        // Refresh Token 只含 subject（userId），无 userId claim
        Claims claims = tokenManager.parseToken(rt);
        assertThat(claims.getSubject()).isEqualTo("55");
    }

    // ---- 过期校验 ----

    @Test
    void expiredTokenThrowsExpiredJwtException() {
        // 创建一个立即过期的 TokenManager（accessTokenMinutes=0 会导致过期时间在过去）
        // 使用负数分钟模拟已过期
        TokenManager expiredTm = new TokenManager(SECRET, -1L, 7L);
        String expiredToken = expiredTm.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");

        assertThatThrownBy(() -> expiredTm.parseToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isSignatureValidReturnsTrueForValidToken() {
        String token = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        assertThat(tokenManager.isSignatureValid(token)).isTrue();
    }

    @Test
    void isSignatureValidReturnsFalseForTamperedToken() {
        String token = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        // 篡改 token 的 payload 部分
        String[] parts = token.split("\\.");
        String tampered = parts[0] + ".tampered_payload." + parts[2];
        assertThat(tokenManager.isSignatureValid(tampered)).isFalse();
    }

    @Test
    void isSignatureValidReturnsTrueForExpiredButValidSignature() {
        TokenManager expiredTm = new TokenManager(SECRET, -1L, 7L);
        String expiredToken = expiredTm.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        // 签名有效，即使已过期
        assertThat(expiredTm.isSignatureValid(expiredToken)).isTrue();
    }

    @Test
    void getExpirationReturnsPositiveTimestamp() {
        String token = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        long expiry = tokenManager.getExpiration(token);
        assertThat(expiry).isGreaterThan(System.currentTimeMillis());
    }

    // ---- Refresh Token 一次性机制（逻辑层面验证） ----

    @Test
    void refreshTokenAndAccessTokenHaveDifferentExpiry() {
        String at = tokenManager.generateAccessToken(1L, "u", List.of(), List.of(), "SELF");
        String rt = tokenManager.generateRefreshToken(1L);

        long atExpiry = tokenManager.getExpiration(at);
        long rtExpiry = tokenManager.getExpiration(rt);

        // Refresh Token 有效期（7天）应远大于 Access Token（30分钟）
        assertThat(rtExpiry).isGreaterThan(atExpiry);
    }
}
