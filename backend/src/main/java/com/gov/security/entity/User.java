package com.gov.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 用户实体，同时实现 Spring Security UserDetails 接口。
 * 角色编码（ROLE_xxx）与权限编码均注入为 GrantedAuthority。
 * 需求：6.1、6.2
 *
 * <p><b>安全注意：</b>password 字段标注 @JsonIgnore，防止序列化泄露。
 */
@Data
public class User implements UserDetails {

    private Long id;
    private String username;

    @JsonIgnore
    private String password;

    private String phone;
    private String realName;

    /** 状态：1启用 0禁用 */
    private Integer status;

    private Long deptId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---- 运行时注入，不持久化到数据库 ----

    /** 角色编码列表，如 ROLE_CITIZEN */
    @JsonIgnore
    private transient List<String> roleCodes = Collections.emptyList();

    /** 权限编码列表，如 user:add */
    @JsonIgnore
    private transient List<String> permissionCodes = Collections.emptyList();

    /**
     * 账号是否被锁定（由 Redis account_lock 控制）。
     * 注入此标志位，让 Spring Security 内置机制也能感知锁定状态。
     */
    @JsonIgnore
    private transient boolean accountLocked = false;

    // ---- UserDetails 接口实现 ----

    /**
     * 将角色编码与权限编码合并为 GrantedAuthority 集合。
     * 需求：6.1、6.2
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
                roleCodes.stream().map(SimpleGrantedAuthority::new),
                permissionCodes.stream().map(SimpleGrantedAuthority::new)
        ).toList();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        // 联动 Redis 锁定标志，让 Spring Security 内置机制也能感知
        return !accountLocked;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号是否启用，对应 status = 1 */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
