package com.gov.security.service;

import com.gov.security.controller.dto.MenuVO;
import com.gov.security.entity.Menu;
import com.gov.security.entity.User;
import com.gov.security.mapper.MenuMapper;
import com.gov.security.mapper.PermissionMapper;
import com.gov.security.entity.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 路由与菜单服务
 * 需求：8.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;

    /**
     * 获取当前登录用户的菜单树与按钮权限列表
     *
     * @return Map 包含 menus（菜单树）和 permissions（按钮权限编码列表）
     */
    public Map<String, Object> getMenusAndPermissions() {
        Long userId = getCurrentUserId();
        log.debug("查询用户菜单与权限: userId={}", userId);

        // 查询用户所有权限（menu + button）
        List<Permission> allPermissions = permissionMapper.findByUserId(userId);

        // 按钮权限：type = 'button'
        List<String> buttonPermissions = allPermissions.stream()
                .filter(p -> "button".equals(p.getType()))
                .map(Permission::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());

        // 查询用户有权访问的菜单
        List<Menu> menus = menuMapper.findByUserId(userId);

        // 构建菜单树（type=0目录 或 type=1菜单）
        List<MenuVO> menuTree = buildMenuTree(menus);

        return Map.of("menus", menuTree, "permissions", buttonPermissions);
    }

    /**
     * 将菜单列表构建为树形结构
     * parentId = 0 或 null 的为根节点
     */
    private List<MenuVO> buildMenuTree(List<Menu> menus) {
        // 过滤出目录和菜单（type=0或1），排除按钮（type=2）
        List<Menu> menuItems = menus.stream()
                .filter(m -> m.getType() != null && m.getType() < 2)
                .collect(Collectors.toList());

        // 转换为 VO 并建立 id -> VO 映射
        Map<Long, MenuVO> voMap = menuItems.stream()
                .collect(Collectors.toMap(Menu::getId, this::toMenuVO));

        List<MenuVO> roots = new ArrayList<>();

        for (Menu menu : menuItems) {
            MenuVO vo = voMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(vo);
            } else {
                MenuVO parent = voMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父节点不在当前用户权限范围内，作为根节点处理
                    roots.add(vo);
                }
            }
        }

        return roots;
    }

    private MenuVO toMenuVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        return vo;
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }
}
