package com.gov.security.controller.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单视图对象（用于前端动态路由）
 * 需求：8.1
 */
@Data
public class MenuVO {
    private Long id;
    private String name;
    private String path;
    private String component;
    private String icon;
    private List<MenuVO> children = new ArrayList<>();
}
