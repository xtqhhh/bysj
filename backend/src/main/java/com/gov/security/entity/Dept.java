package com.gov.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体
 * 对应数据库 dept 表
 */
@Data
public class Dept {
    private Long id;
    private String deptName;
    /** 父部门ID，0 表示顶级 */
    private Long parentId;
    private Integer orderNum;
    /** 状态：1启用 0禁用 */
    private Integer status;
    private LocalDateTime createTime;
}
