package com.gov.security.controller;

import com.gov.security.common.response.ApiResponse;
import com.gov.security.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 路由与菜单控制器
 * 需求：8.1
 */
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * GET /api/route/menus
     * 根据当前登录用户的角色与权限，返回菜单树及按钮权限标识列表
     * 需要认证（携带有效 Token），不需要特定角色
     * 需求：8.1
     */
    @GetMapping("/menus")
    public ApiResponse<Map<String, Object>> getMenus() {
        return ApiResponse.success(routeService.getMenusAndPermissions());
    }
}
