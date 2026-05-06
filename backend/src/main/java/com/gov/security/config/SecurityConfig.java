package com.gov.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.security.common.response.ApiResponse;
import com.gov.security.common.response.ErrorCode;
import com.gov.security.security.JwtAuthenticationFilter;
import com.gov.security.security.XssFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 核心配置
 *
 * <p><b>安全注意：</b>密码字段（password）严禁出现在任何日志输出、响应体或数据库明文字段中。
 * 所有密码操作必须通过 {@link com.gov.security.security.PasswordEncoderUtil} 进行加密处理。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.bcrypt.strength:10}")
    private int bcryptStrength;

    // YAML list 不能直接用 @Value 注入，通过构造器从 Environment 读取
    private final List<String> corsAllowedOrigins;
    private final ObjectMapper objectMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final XssFilter xssFilter;

    public SecurityConfig(ObjectMapper objectMapper,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          XssFilter xssFilter,
                          org.springframework.core.env.Environment env) {
        this.objectMapper = objectMapper;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.xssFilter = xssFilter;
        // 读取 YAML list，格式：security.cors.allowed-origins[0], [1], ...
        List<String> origins = new java.util.ArrayList<>();
        int i = 0;
        while (true) {
            String val = env.getProperty("security.cors.allowed-origins[" + i + "]");
            if (val == null) break;
            origins.add(val);
            i++;
        }
        // 兜底：至少允许本地开发地址
        if (origins.isEmpty()) {
            origins.add("http://localhost:5173");
            origins.add("http://localhost:3000");
        }
        this.corsAllowedOrigins = origins;
    }

    /**
     * 注册 XssFilter，优先级最高，在 Spring Security 过滤链之前执行
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>(xssFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * CORS 配置：仅允许白名单域名发起跨域请求（来自 security.cors.allowed-origins）。
     *
     * <p>在 JWT 无状态认证模式下，传统 Session CSRF Token 已禁用（见下方 csrf.disable()）。
     * 跨域状态变更请求（POST、PUT、DELETE）的安全性由 JWT_Filter 强制校验
     * {@code Authorization: Bearer <token>} 请求头来保障，以此替代传统 CSRF Token 机制。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * BCrypt 密码编码器，cost factor 由配置项 security.bcrypt.strength 控制（默认 10）。
     *
     * <p><b>警告：</b>严禁将原始密码（rawPassword）记录到任何日志中。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * 暴露 AuthenticationManager 供登录服务使用。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 已禁用：系统采用 JWT 无状态认证，Authorization 请求头替代传统 CSRF Token
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/captcha",
                    "/api/messages",
                    "/api/applications/query"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.TOKEN_MISSING))
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.ACCESS_DENIED))
                    );
                })
            );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
