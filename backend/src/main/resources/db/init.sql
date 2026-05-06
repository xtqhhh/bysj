-- 创建数据库
CREATE DATABASE IF NOT EXISTS gov_security DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gov_security;

-- 部门表
CREATE TABLE IF NOT EXISTS dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dept_name   VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID（0表示顶级）',
    order_num   INT          NOT NULL DEFAULT 0 COMMENT '显示排序',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    real_name   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    dept_id     BIGINT       COMMENT '所属部门ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码（如ROLE_CITIZEN）',
    data_scope  VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT '数据权限：ALL/DEPT/SELF',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    permission_name VARCHAR(50)  NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码（如user:add）',
    type            VARCHAR(10)  NOT NULL COMMENT '类型：menu/button',
    parent_id       BIGINT       COMMENT '父权限ID',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 菜单表
CREATE TABLE IF NOT EXISTS menu (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    menu_name       VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    path            VARCHAR(200) COMMENT '路由路径',
    component       VARCHAR(200) COMMENT '前端组件路径',
    icon            VARCHAR(100) COMMENT '图标',
    type            TINYINT(1)   NOT NULL COMMENT '类型：0目录 1菜单 2按钮',
    permission_code VARCHAR(100) COMMENT '关联权限编码',
    order_num       INT          NOT NULL DEFAULT 0 COMMENT '显示排序',
    status          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_permission (
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id    BIGINT      NOT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    request_ip     VARCHAR(50) NOT NULL COMMENT '客户端IP',
    operation_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    result         VARCHAR(20) NOT NULL COMMENT '操作结果：SUCCESS/FAIL',
    before_data    TEXT        COMMENT '变更前数据快照（JSON）',
    after_data     TEXT        COMMENT '变更后数据快照（JSON）',
    PRIMARY KEY (id),
    KEY idx_operator_id (operator_id),
    KEY idx_operation_type (operation_type),
    KEY idx_operation_time (operation_time),
    KEY idx_operator_time (operator_id, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 初始化角色数据
INSERT INTO role (role_name, role_code, data_scope) VALUES
    ('群众用户',   'ROLE_CITIZEN',     'SELF'),
    ('普通公务员', 'ROLE_OFFICER',     'DEPT'),
    ('超级管理员', 'ROLE_SUPER_ADMIN', 'ALL')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), data_scope = VALUES(data_scope);

-- 初始化顶级部门
INSERT INTO dept (dept_name, parent_id, order_num) VALUES ('政务服务中心', 0, 1)
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name);

-- 留言板表
CREATE TABLE IF NOT EXISTS message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '留言人ID',
    user_name   VARCHAR(50)  NOT NULL COMMENT '留言人用户名',
    real_name   VARCHAR(50)  COMMENT '留言人真实姓名',
    content     VARCHAR(500) NOT NULL COMMENT '留言内容',
    reply       VARCHAR(500) COMMENT '管理员回复',
    reply_time  DATETIME     COMMENT '回复时间',
    audit_status TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1已通过 2已拒绝',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '留言时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_audit_status (audit_status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言板';

-- 政务申请表
CREATE TABLE IF NOT EXISTS gov_application (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_no     VARCHAR(30)  NOT NULL COMMENT '申请编号',
    user_id      BIGINT       NOT NULL COMMENT '申请人ID',
    user_name    VARCHAR(50)  NOT NULL COMMENT '申请人用户名',
    real_name    VARCHAR(50)  COMMENT '申请人真实姓名',
    type         VARCHAR(50)  NOT NULL COMMENT '申请类型',
    title        VARCHAR(200) NOT NULL COMMENT '申请标题',
    description  TEXT         COMMENT '申请描述',
    status       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0待受理 1审核中 2已通过 3已拒绝 4已完成',
    remark       VARCHAR(500) COMMENT '审核备注',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    update_time  DATETIME     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_apply_no (apply_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政务申请表';
