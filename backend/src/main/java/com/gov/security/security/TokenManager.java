package com.gov.security.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT Token 管理器
 * 使用 HS256 算法，密钥 ≥ 256 位
 * 需求：3.7
 */
@Slf4j
@Component
public class TokenManager {

    // Payload 字段：sub（userId）、username、roles、permissions、dataScope、jti、iat、exp
    // userId 通过标准 sub 字段承载，不再单独添加自定义 userId claim，避免冗余。
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_DATA_SCOPE = "dataScope";

    private final SecretKey secretKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public TokenManager(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:30}") long accessTokenMinutes,
            @Value("${jwt.refresh-token-expiration:7}") long refreshTokenDays) {
        // 密钥长度 ≥ 256 位（32 字节）
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    /**
     * 生成 Access Token（有效期 30 分钟）
     */
    public String generateAccessToken(Long userId, String username,
                                       List<String> roles, List<String> permissions,
                                       String dataScope) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenMinutes * 60 * 1000L);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .claim(CLAIM_DATA_SCOPE, dataScope)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token（有效期 7 天）
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenDays * 24 * 60 * 60 * 1000L);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     * 签名无效时抛出 JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 签名是否有效（不校验过期）
     */
    public boolean isSignatureValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 签名有效但已过期，签名本身是合法的
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 获取 Token 的 jti（JWT ID）
     */
    public String getJti(String token) {
        try {
            return parseToken(token).getId();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getId();
        }
    }

    /**
     * 获取 Token 的过期时间（毫秒时间戳）
     */
    public long getExpiration(String token) {
        try {
            return parseToken(token).getExpiration().getTime();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration().getTime();
        }
    }

    /**
     * 获取 Token 中的 userId（从标准 sub 字段读取）
     */
    public Long getUserId(String token) {
        try {
            Claims claims = parseToken(token);
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            return Long.parseLong(e.getClaims().getSubject());
        }
    }

    public TokenClaims extractClaims(String token) {
        Claims claims = parseToken(token);
        return buildTokenClaims(claims);
    }

    private TokenClaims buildTokenClaims(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get(CLAIM_ROLES);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get(CLAIM_PERMISSIONS);
        String dataScope = claims.get(CLAIM_DATA_SCOPE, String.class);
        return new TokenClaims(userId, username, roles, permissions, dataScope, claims.getId());
    }

    public record TokenClaims(Long userId, String username, List<String> roles,
                               List<String> permissions, String dataScope, String jti) {}
}
