package com.gov.security.controller;

import com.gov.security.common.response.ApiResponse;
import com.gov.security.service.AuthService;
import com.gov.security.service.CaptchaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 公开接口：/api/auth/captcha, /api/auth/register, /api/auth/login, /api/auth/refresh
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthService authService;

    /**
     * GET /api/auth/captcha
     */
    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> captcha() {
        CaptchaService.CaptchaResult result = captchaService.generate();
        return ApiResponse.success(Map.of("uuid", result.uuid(), "image", result.image()));
    }

    /**
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.getUsername(), req.getPassword(), req.getPhone(), req.getRealName());
        return ApiResponse.success("注册成功");
    }

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(
                req.getUsername(), req.getPassword(), req.getCaptchaUuid(), req.getCaptchaCode()));
    }

    /**
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest req) {
        String newAccessToken = authService.refresh(req.getRefreshToken());
        return ApiResponse.success(Map.of("accessToken", newAccessToken));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ApiResponse.success("登出成功");
    }

    /**
     * PUT /api/auth/password
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                             @RequestHeader("Authorization") String authHeader) {
        authService.changePassword(authHeader, req.getCurrentPassword(), req.getNewPassword());
        return ApiResponse.success("密码修改成功");
    }

    // ---- Request DTOs ----

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "真实姓名不能为空")
        private String realName;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "验证码UUID不能为空")
        private String captchaUuid;
        @NotBlank(message = "验证码不能为空")
        private String captchaCode;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "refreshToken不能为空")
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "当前密码不能为空")
        private String currentPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
