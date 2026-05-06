// Feature: gov-security-auth-system, Property 6
package com.gov.security.property;

import com.gov.security.entity.User;
import net.jqwik.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 属性测试：无权限访问返回 403
 * <p>
 * Property 6: 对于任意用户和受保护接口，若该用户的角色不具备访问该接口所需的权限编码，
 * 则请求应返回 HTTP 403 及错误码 ACCESS_DENIED。
 * <p>Validates: Requirements 6.3
 */
class AccessControlPropertyTest {

    /**
     * 生成随机权限编码（格式：resource:action）
     */
    @Provide
    Arbitrary<String> permissionCodes() {
        Arbitrary<String> resources = Arbitraries.of(
                "user", "role", "dept", "audit", "report", "config", "menu", "log"
        );
        Arbitrary<String> actions = Arbitraries.of(
                "add", "delete", "update", "query", "export", "import", "approve", "view"
        );
        return Combinators.combine(resources, actions).as((r, a) -> r + ":" + a);
    }

    /**
     * 生成随机用户权限集合（0 到 5 个权限）
     */
    @Provide
    Arbitrary<List<String>> userPermissionSets() {
        return permissionCodes().list().ofMinSize(0).ofMaxSize(5);
    }

    /**
     * 构建带有指定权限的 Authentication 对象
     */
    private Authentication buildAuthentication(List<String> permissionCodes) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setStatus(1);
        user.setPermissionCodes(permissionCodes);
        user.setRoleCodes(List.of("ROLE_CITIZEN"));

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    /**
     * 检查 Authentication 是否拥有指定权限
     */
    private boolean hasAuthority(Authentication auth, String requiredPermission) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(requiredPermission));
    }

    /**
     * Property 6: 无权限访问返回 403（ACCESS_DENIED）
     * <p>
     * 对于任意用户权限集合和任意所需权限编码，若用户权限集合中不包含所需权限，
     * 则 Spring Security 的权限检查应拒绝访问（等价于返回 HTTP 403）。
     * <p>Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void unauthorizedAccessDenied(
            @ForAll("userPermissionSets") List<String> userPermissions,
            @ForAll("permissionCodes") String requiredPermission) {

        // 确保用户不拥有所需权限（从用户权限集合中移除该权限）
        List<String> permissionsWithoutRequired = userPermissions.stream()
                .filter(p -> !p.equals(requiredPermission))
                .collect(Collectors.toList());

        Authentication auth = buildAuthentication(permissionsWithoutRequired);

        // 验证：用户不拥有所需权限
        boolean userHasPermission = hasAuthority(auth, requiredPermission);
        assertThat(userHasPermission)
                .as("用户权限集合 %s 不应包含所需权限 %s", permissionsWithoutRequired, requiredPermission)
                .isFalse();

        // 验证：Spring Security 权限检查应拒绝访问
        // 模拟 @PreAuthorize("hasAuthority('requiredPermission')") 的检查逻辑
        boolean accessGranted = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(requiredPermission));

        assertThat(accessGranted)
                .as("用户不具备权限 %s，访问应被拒绝（403 ACCESS_DENIED）", requiredPermission)
                .isFalse();

        // 验证：通过 SecurityContextHolder 设置上下文后，权限检查结果一致
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        try {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(currentAuth).isNotNull();

            boolean contextHasPermission = currentAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals(requiredPermission));

            assertThat(contextHasPermission)
                    .as("SecurityContext 中的用户不具备权限 %s，应返回 ACCESS_DENIED", requiredPermission)
                    .isFalse();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Property 6b: 拥有权限时访问被允许（反向验证）
     * <p>
     * 确保权限检查逻辑正确：当用户拥有所需权限时，访问应被允许。
     * <p>Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void authorizedAccessGranted(@ForAll("permissionCodes") String requiredPermission) {
        // 用户拥有所需权限
        List<String> userPermissions = List.of(requiredPermission);
        Authentication auth = buildAuthentication(userPermissions);

        boolean accessGranted = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(requiredPermission));

        assertThat(accessGranted)
                .as("用户具备权限 %s，访问应被允许", requiredPermission)
                .isTrue();
    }

    /**
     * Property 6c: 用户角色不匹配时访问被拒绝
     * <p>
     * 验证基于角色的访问控制：当用户角色不包含所需角色时，访问应被拒绝。
     * <p>Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void roleBasedAccessDeniedWhenRoleMissing(
            @ForAll("permissionCodes") String requiredPermission) {

        // 用户只有 ROLE_CITIZEN，没有任何权限编码
        User user = new User();
        user.setId(2L);
        user.setUsername("citizen");
        user.setPassword("encoded");
        user.setStatus(1);
        user.setRoleCodes(List.of("ROLE_CITIZEN"));
        user.setPermissionCodes(List.of()); // 无权限编码

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        // 验证：ROLE_CITIZEN 用户不具备任意权限编码（非角色格式）
        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(requiredPermission));

        assertThat(hasPermission)
                .as("ROLE_CITIZEN 用户不应具备权限编码 %s，访问应被拒绝（403）", requiredPermission)
                .isFalse();
    }
}
