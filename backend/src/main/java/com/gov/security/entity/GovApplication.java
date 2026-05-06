package com.gov.security.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 政务申请实体
 * 状态：0待受理 1审核中 2已通过 3已拒绝 4已完成
 */
@Data
public class GovApplication {
    private Long id;
    private String applyNo;
    private Long userId;
    private String userName;
    private String realName;
    private String type;
    private String title;
    private String description;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
