package com.gov.security.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分页查询结果 VO
 * 需求：9.1
 */
@Data
public class UserPageVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    /** 状态：1启用 0禁用 */
    private Integer status;
    /** 角色编码列表 */
    private List<String> roles;
    private String deptName;
    private LocalDateTime createTime;
}
