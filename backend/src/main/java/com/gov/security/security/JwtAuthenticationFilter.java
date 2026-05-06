package com.gov.security.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.security.common.constant.RedisKeyConstants;
import com.gov.security.common.response.ApiResponse;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * JWT 认证过滤器
 * 继承 OncePerRequestFilter，拦截所有受保护请求
 * 需求：3.1 ~ 3.6
 *
 * 权限实时性修复：不再完全信任 JWT Payload 中的权限列表，
 * 而是从 Redis 权限缓存实时加载，确保角色变更后立即生效。
 * @Lazy 注入 UserService 打破循环依赖：
 *   SecurityConfig → JwtAuthenticationFilter → UserService → PasswordEncoder(SecurityConfig)
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(TokenManager tokenManager,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper,
                                    @Lazy UserService userService) {
        this.tokenManager = tokenManager;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // 3.3 Token 缺失时放行，由 Spring Security 决定是否拦截
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3.4 校验签名并解析 claims
            TokenManager.TokenClaims claims = tokenManager.extractClaims(token);

            // 3.6 检查黑名单（已吊销）
            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(claims.jti());
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                writeError(response, ErrorCode.TOKEN_REVOKED);
                return;
            }

            // 权限实时性修复：从 Redis 缓存实时加载权限
            // 角色变更后权限立即生效，不需要等 token 过期
            List<String> roles = claims.roles();
            List<String> permissions;
            try {
                permissions = userService.getPermissionCodes(claims.userId());
            } catch (Exception e) {
                // 缓存/DB 异常时降级使用 token 中的权限
                log.warn("实时加载权限失败，降级使用 token payload: userId={}", claims.userId());
                permissions = claims.permissions();
            }

            // 3.2 写入 SecurityContextHolder
            List<SimpleGrantedAuthority> authorities = Stream.concat(
                    roles.stream().map(SimpleGrantedAuthority::new),
                    permissions.stream().map(SimpleGrantedAuthority::new)
            ).toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // 3.5 Token 已过期
            writeError(response, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            // 3.4 签名无效
            writeError(response, ErrorCode.TOKEN_INVALID);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }
}
