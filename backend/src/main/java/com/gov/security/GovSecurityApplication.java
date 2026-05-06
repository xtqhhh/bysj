package com.gov.security;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 政务管理系统安全认证与权限控制平台 - 主应用类
 */
@SpringBootApplication
@MapperScan("com.gov.security.mapper")
@EnableAsync
public class GovSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovSecurityApplication.class, args);
    }
}
