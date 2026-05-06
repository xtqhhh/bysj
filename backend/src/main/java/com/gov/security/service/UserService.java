package com.gov.security.service;

import com.gov.security.common.constant.RedisKeyConstants;
import com.gov.security.common.exception.BusinessException;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.controller.dto.CreateUserRequest;
import com.gov.security.controller.dto.UserPageVO;
import com.gov.security.entity.Role;
import com.gov.security.entity.User;
import com.gov.security.mapper.RoleMapper;
import com.gov.security.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    /** 权限缓存服务（可选注入，测试环境可不提供） */
    @Autowired(required = false)
    private PermissionCacheService permissionCacheService;

    /**
     * 用户注册
     * 需求：1.1 ~ 1.6
     */
    @Transactional
    public void register(String username, String rawPassword, String phone, String realName) {
        // 1.3 校验用户名唯一性
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        // 1.4 校验手机号格式
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }
        // 1.5 校验密码强度
        if (rawPassword == null || !rawPassword.matches(PASSWORD_REGEX)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }

        // 1.2 BCrypt 加密密码
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRealName(realName);
        userMapper.insert(user);

        // 1.6 默认分配 ROLE_CITIZEN
        Role citizenRole = roleMapper.findByRoleCode("ROLE_CITIZEN");
        if (citizenRole != null) {
            roleMapper.assignRoleToUser(user.getId(), citizenRole.getId());
        }
        log.info("用户注册成功: username={}", username);
    }

    /**
     * 根据用户名加载用户（含角色与权限）
     */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public List<String> getRoleCodes(Long userId) {
        return userMapper.findRoleCodesByUserId(userId);
    }

    public List<String> getPermissionCodes(Long userId) {
        if (permissionCacheService != null) {
            return permissionCacheService.getPermissionCodes(userId);
        }
        return userMapper.findPermissionCodesByUserId(userId);
    }

    /**
     * 清除用户权限缓存
     * 需求：6.5 — 角色变更时立即清除对应用户的权限缓存
     */
    public void clearPermissionCache(Long userId) {
        if (permissionCacheService != null) {
            permissionCacheService.clearPermissionCache(userId);
        }
    }

    /**
     * 使用户的 Refresh Token 失效（角色/权限变更时调用）
     * Access Token 因 JWT Filter 实时查权限缓存，权限已即时生效；
     * 删除 Refresh Token 确保用户下次刷新时重新登录获取新 token。
     */
    public void invalidateRefreshToken(Long userId) {
        redisTemplate.delete(RedisKeyConstants.refreshTokenKey(userId));
    }

    /**
     * 修改用户角色（提权/降权）
     * 清除权限缓存并使 Refresh Token 失效，确保变更立即生效
     */
    @Transactional
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        // 删除旧角色关联
        userMapper.deleteUserRoles(userId);
        // 插入新角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            userMapper.insertUserRoles(userId, roleIds);
        }
        // 清除权限缓存，使角色变更立即生效
        clearPermissionCache(userId);
        // 删除 Refresh Token，下次刷新时重新登录获取新 token
        invalidateRefreshToken(userId);
        log.info("用户角色已修改: userId={}, newRoles={}", userId, roleIds);
    }

    /**
     * 修改密码
     * 需求：13.3、13.4
     */
    @Transactional
    public void changePassword(Long userId, String currentRaw, String newRaw) {
        User user = userMapper.findById(userId);
        // 13.3 验证当前密码
        if (!passwordEncoder.matches(currentRaw, user.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_WRONG);
        }
        // 13.4 校验新密码强度
        if (newRaw == null || !newRaw.matches(PASSWORD_REGEX)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newRaw));
        log.info("用户密码修改成功: userId={}", userId);
    }

    /**
     * 校验密码强度（供属性测试使用）
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        return password.matches(PASSWORD_REGEX);
    }

    // ---- 管理员用户管理 ----

    /**
     * 分页查询用户列表
     * 需求：9.1
     */
    public Map<String, Object> listUsers(String username, String role, Integer status, int page, int size) {
        long offset = (long) (page - 1) * size;
        List<UserPageVO> list = userMapper.findPage(username, role, status, size, offset);
        // 填充每个用户的角色列表
        for (UserPageVO vo : list) {
            vo.setRoles(userMapper.findRolesByUserId(vo.getId()));
        }
        long total = userMapper.countPage(username, role, status);
        return Map.of("total", total, "list", list);
    }

    /**
     * 创建用户（管理员操作）
     * 需求：9.2
     */
    @Transactional
    public Long createUser(CreateUserRequest req) {
        // 校验用户名唯一性
        if (userMapper.findByUsername(req.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        // 校验密码强度
        if (!isStrongPassword(req.getPassword())) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setDeptId(req.getDeptId());
        userMapper.insert(user);

        // 分配角色
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            userMapper.insertUserRoles(user.getId(), req.getRoleIds());
        }
        log.info("管理员创建用户成功: username={}", req.getUsername());
        return user.getId();
    }

    /**
     * 禁用/启用用户，禁用时使该用户所有在线 Token 立即失效
     * 需求：9.3
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        userMapper.updateStatus(userId, status);
        if (status == 0) {
            // 禁用时使所有在线 Token 失效
            invalidateUserTokens(userId);
            log.info("用户已被禁用，Token 已失效: userId={}", userId);
        }
    }

    /**
     * 重置用户密码，同时使该用户所有在线 Token 失效
     * 需求：9.4
     */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        if (!isStrongPassword(newPassword)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        // 使所有在线 Token 失效
        invalidateUserTokens(userId);
        log.info("管理员重置用户密码成功: userId={}", userId);
    }

    /**
     * 使指定用户所有在线 Token 立即失效
     * 通过删除 Refresh Token 并将所有 Access Token 加入黑名单实现
     */
    private void invalidateUserTokens(Long userId) {
        // 删除 Refresh Token
        redisTemplate.delete(RedisKeyConstants.refreshTokenKey(userId));
        // 删除所有 Access Token 存活标记（pattern: access_token:{userId}:*）
        String pattern = RedisKeyConstants.ACCESS_TOKEN_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        // 清除权限缓存
        redisTemplate.delete(RedisKeyConstants.userPermissionsKey(userId));
    }
}
