package com.gov.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleCode;
    /** 数据权限：ALL / DEPT / SELF */
    private String dataScope;
    private LocalDateTime createTime;
}
