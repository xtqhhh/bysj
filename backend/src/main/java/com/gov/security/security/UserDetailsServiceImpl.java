package com.gov.security.security;

import com.gov.security.entity.User;
import com.gov.security.mapper.UserMapper;
import com.gov.security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security UserDetailsService 实现。
 * 从数据库加载用户信息，并将角色与权限注入为 GrantedAuthority。
 * 需求：6.1、6.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.debug("用户不存在: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 加载角色编码
        List<String> roleCodes = userMapper.findRoleCodesByUserId(user.getId());
        // 加载权限编码（优先从 Redis 缓存读取）
        List<String> permissionCodes = userService.getPermissionCodes(user.getId());

        user.setRoleCodes(roleCodes);
        user.setPermissionCodes(permissionCodes);

        // 修复：注入 Redis 账号锁定状态，让 Spring Security 内置机制感知
        boolean locked = Boolean.TRUE.equals(
            redisTemplate.hasKey(com.gov.security.common.constant.RedisKeyConstants.accountLockKey(username)));
        user.setAccountLocked(locked);

        log.debug("加载用户成功: username={}, roles={}, locked={}", username, roleCodes, locked);
        return user;
    }
}
