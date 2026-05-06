# 设计文档：基于 Spring Security 的政务管理系统安全认证与权限控制平台

## 概述

本文档详细说明基于 Spring Security 的政务管理系统安全认证与权限控制平台的总体设计与详细设计方案，明确系统架构、模块划分、数据库结构、安全机制实现方案以及关键技术选型，为系统开发、测试与维护提供依据。

### 系统目标

构建一个具备以下能力的政务服务安全认证与权限控制平台：

- 基于 JWT 的无状态认证系统
- 支持 RBAC 角色权限模型
- 支持数据权限控制（全部 / 本部门 / 本人）
- 支持方法级权限控制（`@PreAuthorize`）
- 完整安全防护能力（验证码、XSS、防暴力破解、Token 刷新、黑名单机制、审计日志等）

---

## 架构

### 总体架构

系统采用前后端分离架构，前端基于 Vue，后端基于 Spring Boot + Spring Security。

```
┌─────────────────────────────────────────────────────────┐
│                        前端（Vue）                        │
│  Vue Router（动态路由）  Axios（拦截器）  v-permission     │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP / HTTPS
┌────────────────────────▼────────────────────────────────┐
│                     后端（Spring Boot）                   │
│                                                          │
│  XSS_Filter → JWT_Filter → Spring Security Filter Chain │
│                         ↓                               │
│              Controller（表现层）                         │
│                         ↓                               │
│              Service（业务层）                            │
│                         ↓                               │
│         Permission_Controller（权限控制层）               │
│                         ↓                               │
│              Mapper（持久层 / MyBatis）                   │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
    ┌──────▼──────┐           ┌───────▼──────┐
    │    MySQL    │           │    Redis     │
    └─────────────┘           └──────────────┘
```

### 认证请求流程

```
客户端请求
    │
    ▼
XSS_Filter（转义特殊字符）
    │
    ▼
JWT_Filter（提取并校验 Token）
    │
    ├─ Token 无效 → 返回 401
    │
    ▼
Spring Security（权限校验）
    │
    ├─ 无权限 → 返回 403
    │
    ▼
Controller → Service → Mapper → MySQL
```

### 技术选型

| 技术 | 版本建议 | 作用 |
|------|---------|------|
| Spring Boot | 3.x | 应用框架 |
| Spring Security | 6.x | 安全框架 |
| JWT（jjwt） | 0.12.x | 无状态认证 |
| Redis | 7.x | Token 缓存、验证码、黑名单、权限缓存 |
| MyBatis | 3.x | ORM 框架 |
| MySQL | 8.x | 数据存储 |
| BCrypt | — | 密码加密（cost ≥ 10） |
| Vue | 3.x | 前端框架 |
| Vue Router | 4.x | 动态路由 |
| Axios | 1.x | HTTP 客户端 |

---

## 组件与接口

### 后端核心组件

#### Auth_System（认证系统）

负责用户注册、登录、登出及 Token 管理。

**注册流程：**

```
校验用户名唯一性 → 校验手机号格式 → 校验密码强度
    → BCrypt 加密密码 → 分配 ROLE_CITIZEN → 写入数据库
```

**登录流程：**

```
校验验证码（Redis）→ 校验账号密码 → 检查失败计数（Redis）
    → 生成 Access_Token + Refresh_Token → 写入 Redis → 返回 Token
```

#### JWT_Filter（JWT 过滤器）

继承 `OncePerRequestFilter`，在 Spring Security 过滤链中拦截所有受保护请求。

处理逻辑：

1. 从 `Authorization: Bearer <token>` 提取 Token
2. 校验签名（HS256）
3. 校验是否过期
4. 查询 Redis 黑名单
5. 解析用户信息写入 `SecurityContextHolder`

#### Permission_Controller（权限控制器）

- RBAC 权限校验：通过 `@PreAuthorize("hasAuthority('xxx')")` 实现方法级控制
- 数据权限：通过 MyBatis 拦截器动态拼接 SQL WHERE 条件
- 权限缓存：用户权限列表缓存于 Redis，角色变更时立即清除

#### XSS_Filter（XSS 过滤器）

继承 `HttpServletRequestWrapper`，对所有请求参数和 JSON 请求体递归转义 HTML 特殊字符。

#### Audit_Logger（审计日志器）

基于 AOP 切面，异步（线程池）写入审计日志表，不阻塞主业务流程。

### 前端核心组件

#### Axios 拦截器设计

```javascript
// 请求拦截器：自动附加 Authorization 头
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：处理 401 TOKEN_EXPIRED，自动刷新 Token
axios.interceptors.response.use(
  response => response,
  async error => {
    const { status, data } = error.response
    if (status === 401 && data.code === 'TOKEN_EXPIRED' && !error.config._retry) {
      error.config._retry = true
      const newToken = await refreshToken()
      error.config.headers.Authorization = `Bearer ${newToken}`
      return axios(error.config)
    }
    return Promise.reject(error)
  }
)
```

#### Token 刷新逻辑

```
前端检测到 401 TOKEN_EXPIRED
    │
    ▼
调用 POST /api/auth/refresh（携带 Refresh_Token）
    │
    ├─ 成功 → 更新本地 Access_Token → 重试原请求
    │
    └─ 失败（REFRESH_TOKEN_INVALID）→ 清除本地 Token → 跳转登录页
```

#### 动态路由注册

```javascript
// 登录成功后，根据后端返回的菜单列表动态注册路由
function buildRoutes(menus) {
  return menus.map(menu => ({
    path: menu.path,
    component: () => import(`@/views/${menu.component}`),
    meta: { permissions: menu.permissions }
  }))
}

router.addRoute('layout', ...buildRoutes(userMenus))
```

#### v-permission 自定义指令

```javascript
app.directive('permission', {
  mounted(el, binding) {
    const userPermissions = store.getters.permissions
    if (!userPermissions.includes(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
})
```

### API 接口设计

#### 认证接口

**获取验证码**

```
GET /api/auth/captcha
Response 200:
{
  "uuid": "string",       // Redis key
  "image": "string"       // Base64 编码图片
}
```

**用户注册**

```
POST /api/auth/register
Request:
{
  "username": "string",
  "password": "string",
  "phone": "string",
  "realName": "string"
}
Response 200: { "message": "注册成功" }
Response 400: { "code": "USER_ALREADY_EXISTS | INVALID_PHONE_FORMAT | WEAK_PASSWORD", "message": "string" }
```

**用户登录**

```
POST /api/auth/login
Request:
{
  "username": "string",
  "password": "string",
  "captchaUuid": "string",
  "captchaCode": "string"
}
Response 200:
{
  "accessToken": "string",
  "refreshToken": "string",
  "user": {
    "id": "long",
    "username": "string",
    "realName": "string",
    "roles": ["string"],
    "permissions": ["string"]
  }
}
Response 400/401: { "code": "CAPTCHA_INVALID | INVALID_CREDENTIALS | ACCOUNT_LOCKED | ACCOUNT_DISABLED", "message": "string" }
```

**Token 刷新**

```
POST /api/auth/refresh
Request: { "refreshToken": "string" }
Response 200: { "accessToken": "string" }
Response 401: { "code": "REFRESH_TOKEN_INVALID", "message": "string" }
```

**用户登出**

```
POST /api/auth/logout
Header: Authorization: Bearer <access_token>
Response 200: { "message": "登出成功" }
```

#### 权限与路由接口

**获取用户菜单与权限**

```
GET /api/route/menus
Header: Authorization: Bearer <access_token>
Response 200:
{
  "menus": [
    {
      "id": "long",
      "name": "string",
      "path": "string",
      "component": "string",
      "icon": "string",
      "children": []
    }
  ],
  "permissions": ["user:add", "user:delete", "..."]
}
```

#### 用户管理接口（仅 ROLE_SUPER_ADMIN）

**分页查询用户**

```
GET /api/admin/users?page=1&size=10&username=&role=&status=
Response 200:
{
  "total": "long",
  "list": [{ "id", "username", "realName", "phone", "status", "roles", "deptName", "createTime" }]
}
```

**创建用户**

```
POST /api/admin/users
Request: { "username", "password", "realName", "phone", "roleIds": ["long"], "deptId": "long" }
Response 200: { "id": "long", "message": "创建成功" }
```

**禁用/启用用户**

```
PUT /api/admin/users/{id}/status
Request: { "status": 0 | 1 }
Response 200: { "message": "操作成功" }
```

**重置用户密码**

```
PUT /api/admin/users/{id}/password
Request: { "newPassword": "string" }
Response 200: { "message": "密码重置成功" }
```

**角色管理**

```
GET    /api/admin/roles                    // 查询角色列表
POST   /api/admin/roles                    // 创建角色
PUT    /api/admin/roles/{id}               // 修改角色
DELETE /api/admin/roles/{id}               // 删除角色（校验是否有关联用户）
PUT    /api/admin/roles/{id}/permissions   // 为角色分配权限
```

#### 审计日志接口（仅 ROLE_SUPER_ADMIN）

```
GET /api/admin/audit-logs?page=1&size=10&operatorId=&type=&startTime=&endTime=
Response 200:
{
  "total": "long",
  "list": [{ "id", "operatorId", "operatorName", "operationType", "requestIp", "operationTime", "result", "beforeData", "afterData" }]
}
```

---

## 数据模型

### 用户表（user）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| username | varchar(50) | UNIQUE, NOT NULL | 用户名 |
| password | varchar(100) | NOT NULL | BCrypt 加密密码 |
| phone | varchar(20) | NOT NULL | 手机号 |
| real_name | varchar(50) | NOT NULL | 真实姓名 |
| status | tinyint(1) | DEFAULT 1 | 状态：1启用 0禁用 |
| dept_id | bigint | FK | 所属部门 ID |
| create_time | datetime | NOT NULL | 创建时间 |
| update_time | datetime | | 更新时间 |

### 角色表（role）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| role_name | varchar(50) | NOT NULL | 角色名称 |
| role_code | varchar(50) | UNIQUE, NOT NULL | 角色编码（如 ROLE_CITIZEN） |
| data_scope | varchar(20) | NOT NULL | 数据权限：ALL / DEPT / SELF |
| create_time | datetime | NOT NULL | 创建时间 |

### 权限表（permission）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| permission_name | varchar(50) | NOT NULL | 权限名称 |
| permission_code | varchar(100) | UNIQUE, NOT NULL | 权限编码（如 user:add） |
| type | varchar(10) | NOT NULL | 类型：menu / button |
| parent_id | bigint | | 父权限 ID |
| create_time | datetime | NOT NULL | 创建时间 |

### 部门表（dept）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| dept_name | varchar(100) | NOT NULL | 部门名称 |
| parent_id | bigint | DEFAULT 0 | 父部门 ID（0 表示顶级） |
| order_num | int | DEFAULT 0 | 显示排序 |
| status | tinyint(1) | DEFAULT 1 | 状态：1启用 0禁用 |
| create_time | datetime | NOT NULL | 创建时间 |

### 菜单表（menu）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| menu_name | varchar(50) | NOT NULL | 菜单名称 |
| parent_id | bigint | DEFAULT 0 | 父菜单 ID |
| path | varchar(200) | | 路由路径 |
| component | varchar(200) | | 前端组件路径 |
| icon | varchar(100) | | 图标 |
| type | tinyint(1) | NOT NULL | 类型：0目录 1菜单 2按钮 |
| permission_code | varchar(100) | | 关联权限编码 |
| order_num | int | DEFAULT 0 | 显示排序 |
| status | tinyint(1) | DEFAULT 1 | 状态：1显示 0隐藏 |
| create_time | datetime | NOT NULL | 创建时间 |

### 中间表

**user_role（用户-角色关联）**

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | bigint | 用户 ID |
| role_id | bigint | 角色 ID |

**role_permission（角色-权限关联）**

| 字段 | 类型 | 说明 |
|------|------|------|
| role_id | bigint | 角色 ID |
| permission_id | bigint | 权限 ID |

### 审计日志表（audit_log）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键 |
| operator_id | bigint | NOT NULL | 操作人 ID |
| operator_name | varchar(50) | NOT NULL | 操作人用户名 |
| operation_type | varchar(50) | NOT NULL | 操作类型 |
| request_ip | varchar(50) | NOT NULL | 客户端 IP |
| operation_time | datetime | NOT NULL | 操作时间 |
| result | varchar(20) | NOT NULL | 操作结果：SUCCESS / FAIL |
| before_data | text | | 变更前数据快照（JSON） |
| after_data | text | | 变更后数据快照（JSON） |

### Redis 数据结构设计

| Key 命名规范 | 数据类型 | TTL | 说明 |
|-------------|---------|-----|------|
| `captcha:{uuid}` | String | 5 分钟 | 图形验证码文本 |
| `access_token:{userId}:{jti}` | String | 30 分钟 | Access_Token 存活标记 |
| `refresh_token:{userId}` | String | 7 天 | Refresh_Token 值 |
| `token_blacklist:{jti}` | String | Token 剩余有效期 | 已吊销 Token 黑名单 |
| `login_fail:{username}` | String（计数） | 10 分钟 | 登录失败次数 |
| `account_lock:{username}` | String | 30 分钟 | 账号锁定标记 |
| `user_permissions:{userId}` | String（JSON） | 30 分钟 | 用户权限列表缓存 |

---

## 正确性属性

*属性（Property）是在系统所有有效执行中都应成立的特征或行为——本质上是对系统应做什么的形式化陈述。属性是人类可读规范与机器可验证正确性保证之间的桥梁。*

### 属性 1：密码加密不可逆且可验证

*对于任意* 有效密码字符串，使用 BCrypt 加密后存储的值应满足：(1) 不等于原始密码明文；(2) 通过 `BCrypt.matches(原始密码, 加密值)` 验证返回 true。

**验证需求：1.2**

### 属性 2：弱密码被拒绝

*对于任意* 密码字符串，若其长度少于 8 位，或仅由字母组成，或仅由数字组成，则注册请求应返回错误码 `WEAK_PASSWORD`，且用户不被创建。

**验证需求：1.5**

### 属性 3：注册成功后默认角色为 ROLE_CITIZEN

*对于任意* 满足格式要求的注册信息（用户名唯一、手机号合法、密码强度足够），注册成功后查询该用户的角色列表，应包含且仅包含 `ROLE_CITIZEN`。

**验证需求：1.6**

### 属性 4：JWT 生成与解析的 Round-Trip 一致性

*对于任意* 用户信息对象（userId、username、roles、permissions、dataScope），将其编码为 JWT 后再解析，得到的用户信息应与原始信息完全一致。

**验证需求：3.2**

### 属性 5：登出后 Token 被加入黑名单

*对于任意* 有效的 Access_Token，调用登出接口后，使用该 Token 访问任意受保护接口，应返回 HTTP 401 及错误码 `TOKEN_REVOKED`。

**验证需求：5.1**

### 属性 6：无权限访问返回 403

*对于任意* 用户和受保护接口，若该用户的角色不具备访问该接口所需的权限编码，则请求应返回 HTTP 403 及错误码 `ACCESS_DENIED`。

**验证需求：6.3**

### 属性 7：数据权限过滤正确性

*对于任意* 数据集合和查询用户，若用户数据权限为 `DEPT`，则查询结果中所有记录的 `dept_id` 应等于该用户的 `dept_id`；若数据权限为 `SELF`，则所有记录的 `create_by` 应等于该用户 ID。

**验证需求：7.2, 7.3**

### 属性 8：Refresh_Token 一次性使用

*对于任意* 有效的 Refresh_Token，使用一次刷新 Access_Token 后，再次使用同一 Refresh_Token 调用刷新接口，应返回错误码 `REFRESH_TOKEN_INVALID`。

**验证需求：4.4**

### 属性 9：审计日志字段完整性

*对于任意* 关键操作（登录、登出、密码修改、角色变更、用户禁用/启用），生成的审计日志记录应包含非空的操作人 ID、操作类型、操作时间、客户端 IP 和操作结果字段。

**验证需求：10.2**

### 属性 10：XSS 过滤转义完整性

*对于任意* 包含 HTML 特殊字符（`<`、`>`、`"`、`'`、`&`）的字符串，经过 XSS_Filter 处理后，这些字符应被替换为对应的 HTML 实体（`&lt;`、`&gt;`、`&quot;`、`&#x27;`、`&amp;`），且不引入新的特殊字符。

**验证需求：12.1**

### 属性 11：JSON 递归 XSS 过滤

*对于任意* 嵌套深度的 JSON 对象，XSS_Filter 递归处理后，所有层级的字符串字段中的 HTML 特殊字符均应被转义，非字符串字段（数字、布尔值、null）应保持不变。

**验证需求：12.2**

### 属性 12：账号锁定阈值

*对于任意* 账号，在 10 分钟窗口内连续登录失败达到 5 次后，第 6 次及后续登录请求应返回错误码 `ACCOUNT_LOCKED`，而不是 `INVALID_CREDENTIALS`。

**验证需求：2.6**

---

## 错误处理

### 异常码列表

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|------------|------|---------|
| `USER_ALREADY_EXISTS` | 400 | 用户名已存在 | 注册时用户名重复 |
| `INVALID_PHONE_FORMAT` | 400 | 手机号格式错误 | 注册/修改时手机号不符合中国大陆格式 |
| `WEAK_PASSWORD` | 400 | 密码强度不足 | 注册/修改密码时密码少于8位或缺少字母数字组合 |
| `CAPTCHA_INVALID` | 400 | 验证码错误或已过期 | 登录时验证码不匹配或 Redis 中已过期 |
| `INVALID_CREDENTIALS` | 401 | 用户名或密码错误 | 登录时凭证不正确（不暴露具体原因） |
| `ACCOUNT_LOCKED` | 401 | 账号已被锁定 | 10分钟内失败5次后触发，锁定30分钟 |
| `ACCOUNT_DISABLED` | 401 | 账号已被禁用 | 管理员禁用该账号后登录 |
| `TOKEN_MISSING` | 401 | Token 缺失 | 请求受保护接口时未携带 Authorization 头 |
| `TOKEN_INVALID` | 401 | Token 签名无效 | JWT 签名校验失败或格式错误 |
| `TOKEN_EXPIRED` | 401 | Token 已过期 | Access_Token 超过30分钟有效期 |
| `TOKEN_REVOKED` | 401 | Token 已被吊销 | Token 存在于 Redis 黑名单中 |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh_Token 无效或已过期 | 刷新接口收到无效或已使用的 Refresh_Token |
| `ACCESS_DENIED` | 403 | 权限不足 | 用户角色不具备访问该接口的权限 |
| `CURRENT_PASSWORD_WRONG` | 400 | 当前密码错误 | 修改密码时原密码验证失败 |
| `ROLE_IN_USE` | 400 | 角色仍有关联用户 | 删除角色时该角色下仍有用户 |

### 全局异常处理

使用 `@RestControllerAdvice` 统一捕获异常，返回标准格式：

```json
{
  "code": "ERROR_CODE",
  "message": "错误描述",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 测试策略

### 双轨测试方法

本系统同时采用单元测试和属性测试，两者互补：

- **单元测试**：验证具体示例、边界条件和错误场景
- **属性测试**：验证跨所有输入的通用属性（基于随机生成的输入）

### 属性测试配置

- 使用 **jqwik**（Java 属性测试库）实现属性测试
- 每个属性测试最少运行 **100 次迭代**
- 每个属性测试通过注释标注对应的设计属性编号

标注格式：`// Feature: gov-security-auth-system, Property {N}: {属性描述}`

### 属性测试覆盖

| 属性编号 | 测试类 | 测试方法 |
|---------|--------|---------|
| 属性 1 | `PasswordEncoderPropertyTest` | `bcryptEncodeAndVerify` |
| 属性 2 | `RegistrationPropertyTest` | `weakPasswordRejected` |
| 属性 3 | `RegistrationPropertyTest` | `defaultRoleIsCitizen` |
| 属性 4 | `JwtPropertyTest` | `jwtRoundTrip` |
| 属性 5 | `LogoutPropertyTest` | `tokenBlacklistedAfterLogout` |
| 属性 6 | `AccessControlPropertyTest` | `unauthorizedAccessDenied` |
| 属性 7 | `DataPermissionPropertyTest` | `dataFilterByScope` |
| 属性 8 | `RefreshTokenPropertyTest` | `refreshTokenSingleUse` |
| 属性 9 | `AuditLogPropertyTest` | `auditLogFieldCompleteness` |
| 属性 10 | `XssFilterPropertyTest` | `specialCharsEscaped` |
| 属性 11 | `XssFilterPropertyTest` | `jsonRecursiveEscape` |
| 属性 12 | `LoginLockPropertyTest` | `accountLockedAfterFiveFailures` |

### 单元测试覆盖

- 验证码生成与 Redis 存储
- 登录流程（验证码优先校验）
- Token 刷新完整流程
- 动态路由菜单生成
- 审计日志 AOP 切面

### 集成测试

- Spring Security 过滤链端到端测试
- MyBatis 数据权限拦截器 SQL 生成验证
- Redis 缓存失效与黑名单机制验证
