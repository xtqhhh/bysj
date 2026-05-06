package com.gov.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体
 * 对应数据库 menu 表
 */
@Data
public class Menu {
    private Long id;
    private String menuName;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    /** 类型：0目录 1菜单 2按钮 */
    private Integer type;
    private String permissionCode;
    private Integer orderNum;
    private Integer status;
    private LocalDateTime createTime;
}
