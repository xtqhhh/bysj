package com.gov.security.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long userId;
    private String userName;
    private String realName;
    private String content;
    private String reply;
    private LocalDateTime replyTime;
    /** 审核状态：0待审核 1已通过 2已拒绝 */
    private Integer auditStatus;
    /** 显示状态：1显示 0隐藏 */
    private Integer status;
    private LocalDateTime createTime;
}
