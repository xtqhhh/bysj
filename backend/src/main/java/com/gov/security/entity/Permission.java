package com.gov.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体
 * 对应数据库 permission 表
 */
@Data
public class Permission {
    private Long id;
    private String permissionName;
    private String permissionCode;
    /** 类型：menu / button */
    private String type;
    private Long parentId;
    private LocalDateTime createTime;
}
