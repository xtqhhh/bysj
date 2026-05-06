package com.gov.security.unit;

import com.gov.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试：权限校验 — 无权限用户应被拒绝（ACCESS_DENIED / 403）
 * 需求：6.3、6.4
 */
class PermissionUnitTest {

    // ---- 无权限访问被拒绝 ----

    @Test
    void userWithoutPermissionIsAccessDenied() {
        Authentication auth = buildAuth(List.of("ROLE_CITIZEN"), List.of());
        boolean hasPermission = hasAuthority(auth, "user:add");
        assertThat(hasPermission).isFalse();
    }

    @Test
    void userWithWrongPermissionIsAccessDenied() {
        Authentication auth = buildAuth(List.of("ROLE_CITIZEN"), List.of("user:read"));
        boolean hasPermission = hasAuthority(auth, "user:delete");
        assertThat(hasPermission).isFalse();
    }

    @Test
    void userWithRequiredPermissionIsGranted() {
        Authentication auth = buildAuth(List.of("ROLE_OFFICER"), List.of("audit:read"));
        boolean hasPermission = hasAuthority(auth, "audit:read");
        assertThat(hasPermission).isTrue();
    }

    @Test
    void superAdminWithAllPermissionsIsGranted() {
        Authentication auth = buildAuth(
                List.of("ROLE_SUPER_ADMIN"),
                List.of("user:add", "user:delete", "role:manage", "audit:read"));
        assertThat(hasAuthority(auth, "user:add")).isTrue();
        assertThat(hasAuthority(auth, "user:delete")).isTrue();
        assertThat(hasAuthority(auth, "role:manage")).isTrue();
    }

    @Test
    void citizenCannotAccessAdminPermission() {
        Authentication auth = buildAuth(List.of("ROLE_CITIZEN"), List.of());
        assertThat(hasAuthority(auth, "user:add")).isFalse();
        assertThat(hasAuthority(auth, "role:manage")).isFalse();
        assertThat(hasAuthority(auth, "audit:read")).isFalse();
    }

    // ---- 角色校验 ----

    @Test
    void citizenRoleIsGrantedForCitizenUser() {
        Authentication auth = buildAuth(List.of("ROLE_CITIZEN"), List.of());
        assertThat(hasAuthority(auth, "ROLE_CITIZEN")).isTrue();
    }

    @Test
    void officerRoleIsNotGrantedForCitizenUser() {
        Authentication auth = buildAuth(List.of("ROLE_CITIZEN"), List.of());
        assertThat(hasAuthority(auth, "ROLE_OFFICER")).isFalse();
    }

    @Test
    void superAdminRoleIsGrantedForSuperAdmin() {
        Authentication auth = buildAuth(List.of("ROLE_SUPER_ADMIN"), List.of());
        assertThat(hasAuthority(auth, "ROLE_SUPER_ADMIN")).isTrue();
    }

    // ---- SecurityContextHolder 集成 ----

    @Test
    void securityContextHolderReflectsUserAuthorities() {
        Authentication auth = buildAuth(List.of("ROLE_OFFICER"), List.of("audit:read"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            assertThat(current).isNotNull();
            assertThat(hasAuthority(current, "audit:read")).isTrue();
            assertThat(hasAuthority(current, "user:delete")).isFalse();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void emptyAuthoritiesAlwaysDenied() {
        Authentication auth = buildAuth(List.of(), List.of());
        assertThat(hasAuthority(auth, "any:permission")).isFalse();
        assertThat(hasAuthority(auth, "ROLE_CITIZEN")).isFalse();
    }

    // ---- helpers ----

    private Authentication buildAuth(List<String> roles, List<String> permissions) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setStatus(1);
        user.setRoleCodes(roles);
        user.setPermissionCodes(permissions);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(authority));
    }
}
