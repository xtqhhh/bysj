package com.gov.security.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.security.common.constant.RedisKeyConstants;
import com.gov.security.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户权限缓存服务
 * 需求：6.5
 *
 * <p>登录时将用户权限列表缓存至 Redis {@code user_permissions:{userId}}，TTL 可通过
 * {@code cache.permission.ttl-minutes} 配置（默认 30 分钟）。
 * 角色变更时立即清除对应用户的权限缓存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    @Value("${cache.permission.ttl-minutes:30}")
    private long permissionCacheTtlMinutes;

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户权限编码列表，优先从 Redis 缓存读取。
     * 缓存未命中时从数据库加载并写入缓存（TTL 由配置决定，默认 30 分钟）。
     *
     * @param userId 用户 ID
     * @return 权限编码列表
     */
    public List<String> getPermissionCodes(Long userId) {
        String cacheKey = RedisKeyConstants.userPermissionsKey(userId);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("权限缓存命中: userId={}", userId);
                return objectMapper.readValue(cached, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("读取权限缓存失败，降级查询数据库: userId={}, error={}", userId, e.getMessage());
        }

        // 缓存未命中，从数据库加载
        List<String> permissions = userMapper.findPermissionCodesByUserId(userId);
        try {
            String json = objectMapper.writeValueAsString(permissions);
            redisTemplate.opsForValue().set(cacheKey, json, permissionCacheTtlMinutes, TimeUnit.MINUTES);
            log.debug("权限已写入缓存: userId={}, count={}, ttl={}min", userId, permissions.size(), permissionCacheTtlMinutes);
        } catch (Exception e) {
            log.warn("写入权限缓存失败: userId={}, error={}", userId, e.getMessage());
        }
        return permissions;
    }

    /**
     * 清除指定用户的权限缓存。
     * 角色变更时调用，确保下次请求重新从数据库加载最新权限。
     *
     * @param userId 用户 ID
     */
    public void clearPermissionCache(Long userId) {
        String cacheKey = RedisKeyConstants.userPermissionsKey(userId);
        Boolean deleted = redisTemplate.delete(cacheKey);
        log.info("清除权限缓存: userId={}, deleted={}", userId, deleted);
    }
}
