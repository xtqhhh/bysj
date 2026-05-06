package com.gov.security.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求 DTO
 * 需求：9.3
 */
@Data
public class UpdateUserStatusRequest {

    /** 状态：1启用 0禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
