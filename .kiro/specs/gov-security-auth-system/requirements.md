# 需求文档

## 简介

本系统为基于 Spring Security 的政务管理系统安全认证与权限控制平台，面向公众提供政务服务。系统支持三类角色：群众用户（提交政务事项）、普通公务员（审核群众提交内容）、超级管理员（管理全局用户与权限）。

系统采用 Spring Boot + Spring Security + JWT + Redis + MyBatis + MySQL 作为后端技术栈，Vue + Vue Router + Axios 作为前端技术栈，实现无状态认证、RBAC 权限模型、数据权限控制、方法级权限控制，以及 BCrypt 加密、图形验证码、CSRF 防护、XSS 防护、操作日志审计、Token 自动刷新、动态路由与按钮级权限控制等安全增强功能。

---

## 词汇表

- **认证系统（Auth_System）**：负责用户身份验证与 Token 管理的核心模块
- **权限控制器（Permission_Controller）**：负责 RBAC 权限模型、数据权限与方法级权限校验的模块
- **JWT 过滤器（JWT_Filter）**：自定义 Spring Security 过滤器，负责解析与校验 JWT Token
- **Token 管理器（Token_Manager）**：负责 JWT 生成、刷新、吊销及 Redis 缓存管理的模块
- **验证码服务（Captcha_Service）**：负责图形验证码生成与校验的模块
- **用户管理器（User_Manager）**：超级管理员使用的用户与角色管理模块
- **审计日志器（Audit_Logger）**：负责记录操作日志与权限变更记录的模块
- **动态路由服务（Route_Service）**：根据用户权限动态生成前端路由与按钮权限的模块
- **XSS 过滤器（XSS_Filter）**：负责过滤请求中 XSS 攻击内容的模块
- **群众用户（Citizen）**：可提交政务事项的普通公众用户
- **公务员（Officer）**：负责审核群众提交内容的普通公务员
- **超级管理员（Super_Admin）**：拥有全局用户与权限管理能力的系统管理员
- **Access_Token**：用于身份认证的短期 JWT，有效期为 30 分钟
- **Refresh_Token**：用于刷新 Access_Token 的长期 JWT，有效期为 7 天
- **RBAC**：基于角色的访问控制模型（Role-Based Access Control）
- **数据权限（Data_Permission）**：限制用户只能访问其权限范围内数据的控制机制

---

## 需求列表

### 需求 1：用户注册

**用户故事：** 作为群众用户，我希望能够注册账号，以便使用政务服务平台提交政务事项。

#### 验收标准

1. THE Auth_System SHALL 提供用户名、密码、手机号、真实姓名的注册接口
2. WHEN 用户提交注册信息时，THE Auth_System SHALL 使用 BCrypt 算法对密码进行加密后存储
3. WHEN 用户提交注册信息时，THE Auth_System SHALL 校验用户名是否已存在，若已存在则返回错误码 `USER_ALREADY_EXISTS` 及描述信息
4. WHEN 用户提交的手机号格式不符合中国大陆手机号规则时，THE Auth_System SHALL 返回错误码 `INVALID_PHONE_FORMAT`
5. WHEN 用户提交的密码长度少于 8 位或不包含字母与数字的组合时，THE Auth_System SHALL 返回错误码 `WEAK_PASSWORD`
6. WHEN 注册成功时，THE Auth_System SHALL 为新用户默认分配 `ROLE_CITIZEN` 角色

---

### 需求 2：用户登录与图形验证码校验

**用户故事：** 作为任意角色用户，我希望通过用户名、密码和图形验证码登录系统，以便安全地访问平台功能。

#### 验收标准

1. THE Captcha_Service SHALL 生成包含随机字符的图形验证码，并将验证码文本以 UUID 为 key 存入 Redis，有效期为 5 分钟
2. WHEN 用户请求验证码时，THE Captcha_Service SHALL 返回验证码图片（Base64 编码）及对应的 UUID
3. WHEN 用户提交登录请求时，THE Auth_System SHALL 优先校验图形验证码，若验证码错误或已过期则返回错误码 `CAPTCHA_INVALID`，且不执行后续认证逻辑
4. WHEN 图形验证码校验通过后，THE Auth_System SHALL 校验用户名与密码
5. WHEN 用户名不存在或密码错误时，THE Auth_System SHALL 返回错误码 `INVALID_CREDENTIALS`，且不暴露具体失败原因
6. WHEN 同一账号在 10 分钟内连续登录失败达到 5 次时，THE Auth_System SHALL 锁定该账号 30 分钟，并返回错误码 `ACCOUNT_LOCKED`
7. WHEN 登录成功时，THE Auth_System SHALL 生成 Access_Token（有效期 30 分钟）与 Refresh_Token（有效期 7 天），并将 Token 信息存入 Redis
8. WHEN 登录成功时，THE Auth_System SHALL 在响应体中返回 Access_Token、Refresh_Token 及用户基本信息
9. WHEN 用户账号处于禁用状态时，THE Auth_System SHALL 返回错误码 `ACCOUNT_DISABLED`

---

### 需求 3：JWT 无状态认证

**用户故事：** 作为系统，我希望通过 JWT 实现无状态认证，以便在不依赖服务端 Session 的情况下验证用户身份。

#### 验收标准

1. THE JWT_Filter SHALL 拦截所有需要认证的 HTTP 请求，从请求头 `Authorization: Bearer <token>` 中提取 Access_Token
2. WHEN Access_Token 存在且签名有效时，THE JWT_Filter SHALL 解析 Token 中的用户信息并写入 Spring Security 上下文
3. WHEN Access_Token 不存在时，THE JWT_Filter SHALL 返回 HTTP 401 状态码及错误码 `TOKEN_MISSING`
4. WHEN Access_Token 签名无效时，THE JWT_Filter SHALL 返回 HTTP 401 状态码及错误码 `TOKEN_INVALID`
5. WHEN Access_Token 已过期时，THE JWT_Filter SHALL 返回 HTTP 401 状态码及错误码 `TOKEN_EXPIRED`
6. WHEN Access_Token 已被吊销（存在于 Redis 黑名单中）时，THE JWT_Filter SHALL 返回 HTTP 401 状态码及错误码 `TOKEN_REVOKED`
7. THE Token_Manager SHALL 使用 HS256 算法对 JWT 进行签名，密钥长度不少于 256 位

---

### 需求 4：Token 自动刷新

**用户故事：** 作为已登录用户，我希望系统能够自动刷新 Token，以便在不重新登录的情况下保持会话连续性。

#### 验收标准

1. THE Token_Manager SHALL 提供 Token 刷新接口，接受有效的 Refresh_Token 并返回新的 Access_Token
2. WHEN Refresh_Token 有效且未过期时，THE Token_Manager SHALL 生成新的 Access_Token 并更新 Redis 中的 Token 记录
3. WHEN Refresh_Token 已过期或无效时，THE Token_Manager SHALL 返回错误码 `REFRESH_TOKEN_INVALID`，要求用户重新登录
4. WHEN 同一 Refresh_Token 被使用一次后，THE Token_Manager SHALL 使该 Refresh_Token 失效，防止重放攻击
5. WHEN 前端收到 HTTP 401 且错误码为 `TOKEN_EXPIRED` 时，THE Route_Service SHALL 自动调用刷新接口，刷新成功后重试原请求

---

### 需求 5：用户登出

**用户故事：** 作为已登录用户，我希望能够安全登出，以便防止 Token 被滥用。

#### 验收标准

1. WHEN 用户请求登出时，THE Auth_System SHALL 将当前 Access_Token 加入 Redis 黑名单，黑名单有效期与 Token 剩余有效期一致
2. WHEN 用户请求登出时，THE Auth_System SHALL 从 Redis 中删除对应的 Refresh_Token
3. WHEN 登出成功时，THE Auth_System SHALL 返回 HTTP 200 状态码及成功标识

---

### 需求 6：RBAC 角色权限控制

**用户故事：** 作为系统，我希望基于 RBAC 模型控制用户对接口的访问，以便不同角色只能访问其被授权的功能。

#### 验收标准

1. THE Permission_Controller SHALL 支持三种系统角色：`ROLE_CITIZEN`（群众用户）、`ROLE_OFFICER`（普通公务员）、`ROLE_SUPER_ADMIN`（超级管理员）
2. THE Permission_Controller SHALL 支持将权限（Permission）分配给角色，将角色分配给用户
3. WHEN 用户访问受保护接口时，THE Permission_Controller SHALL 校验用户角色是否具备对应权限，若无权限则返回 HTTP 403 状态码及错误码 `ACCESS_DENIED`
4. THE Permission_Controller SHALL 支持方法级权限控制，通过注解（如 `@PreAuthorize`）在 Service 层或 Controller 层声明所需权限
5. WHEN 超级管理员修改用户角色时，THE Permission_Controller SHALL 使该用户在 Redis 中的权限缓存立即失效

---

### 需求 7：数据权限控制

**用户故事：** 作为系统，我希望限制用户只能访问其权限范围内的数据，以便保护政务数据安全。

#### 验收标准

1. THE Permission_Controller SHALL 支持数据权限维度：全部数据、本部门数据、本人数据
2. WHEN 公务员查询政务事项列表时，THE Permission_Controller SHALL 根据其数据权限范围过滤查询结果，仅返回其有权访问的数据
3. WHEN 群众用户查询政务事项时，THE Permission_Controller SHALL 仅返回该用户本人提交的数据
4. WHEN 超级管理员查询任意数据时，THE Permission_Controller SHALL 不施加数据权限过滤

---

### 需求 8：动态路由与按钮级权限控制

**用户故事：** 作为已登录用户，我希望前端根据我的权限动态展示菜单和操作按钮，以便只看到我有权使用的功能。

#### 验收标准

1. WHEN 用户登录成功后，THE Route_Service SHALL 根据用户角色与权限返回该用户可访问的菜单列表及按钮权限标识列表
2. THE Route_Service SHALL 在前端通过 Vue Router 动态注册路由，未授权路由不得被直接访问
3. WHEN 用户直接访问未授权路由时，THE Route_Service SHALL 将用户重定向至 403 页面
4. THE Route_Service SHALL 支持按钮级权限控制，通过自定义指令（如 `v-permission`）控制按钮的显示与隐藏
5. WHEN 用户权限发生变更时，THE Route_Service SHALL 在用户下次登录时重新加载路由与按钮权限

---

### 需求 9：超级管理员用户管理

**用户故事：** 作为超级管理员，我希望能够管理系统中的所有用户与角色，以便维护系统的权限体系。

#### 验收标准

1. THE User_Manager SHALL 提供用户列表查询接口，支持按用户名、角色、状态分页查询
2. WHEN 超级管理员创建用户时，THE User_Manager SHALL 支持指定用户名、密码、角色、所属部门
3. WHEN 超级管理员禁用用户时，THE User_Manager SHALL 将该用户状态设为禁用，并使其所有在线 Token 立即失效
4. WHEN 超级管理员重置用户密码时，THE User_Manager SHALL 使用 BCrypt 加密新密码并更新数据库，同时使该用户所有在线 Token 立即失效
5. THE User_Manager SHALL 提供角色管理接口，支持创建、修改、删除角色及为角色分配权限
6. WHEN 超级管理员删除角色时，THE User_Manager SHALL 校验该角色是否仍有关联用户，若有则返回错误码 `ROLE_IN_USE`

---

### 需求 10：操作日志审计

**用户故事：** 作为超级管理员，我希望系统记录所有关键操作日志，以便进行安全审计与问题追溯。

#### 验收标准

1. THE Audit_Logger SHALL 记录以下操作的日志：用户登录、登出、登录失败、密码修改、角色变更、权限变更、用户禁用/启用
2. WHEN 关键操作发生时，THE Audit_Logger SHALL 记录操作人、操作类型、操作时间、客户端 IP、操作结果及变更前后的数据快照
3. THE Audit_Logger SHALL 以异步方式写入日志，不阻塞主业务流程
4. THE Audit_Logger SHALL 提供日志查询接口，支持按操作人、操作类型、时间范围分页查询，仅超级管理员可访问
5. WHEN 权限变更发生时，THE Audit_Logger SHALL 单独记录权限变更记录，包含变更前角色列表与变更后角色列表

---

### 需求 11：CSRF 防护

**用户故事：** 作为系统，我希望防止跨站请求伪造攻击，以便保护用户的操作安全。

#### 验收标准

1. THE Auth_System SHALL 对所有状态变更类请求（POST、PUT、DELETE）启用 CSRF 防护
2. WHERE 系统采用 JWT 无状态认证模式，THE Auth_System SHALL 通过验证请求头中的 `Authorization` Token 替代传统 CSRF Token 机制，确保跨域请求必须携带有效 JWT
3. THE Auth_System SHALL 配置 CORS 策略，仅允许白名单域名发起跨域请求

---

### 需求 12：XSS 防护

**用户故事：** 作为系统，我希望过滤用户输入中的 XSS 攻击内容，以便防止恶意脚本注入。

#### 验收标准

1. THE XSS_Filter SHALL 拦截所有 HTTP 请求，对请求参数与请求体中的 HTML 特殊字符（`<`、`>`、`"`、`'`、`&`）进行转义处理
2. WHEN 请求体为 JSON 格式时，THE XSS_Filter SHALL 递归处理 JSON 中所有字符串字段的 XSS 过滤
3. THE XSS_Filter SHALL 在响应头中设置 `X-XSS-Protection: 1; mode=block` 及 `Content-Security-Policy` 策略

---

### 需求 13：密码安全

**用户故事：** 作为系统，我希望对用户密码进行安全存储与强度校验，以便防止密码泄露与弱密码风险。

#### 验收标准

1. THE Auth_System SHALL 使用 BCrypt 算法（cost factor 不低于 10）对所有用户密码进行加密存储
2. THE Auth_System SHALL 禁止在任何日志、响应体或数据库明文字段中存储或输出用户密码
3. WHEN 用户修改密码时，THE Auth_System SHALL 要求用户提供当前密码进行验证，验证失败则返回错误码 `CURRENT_PASSWORD_WRONG`
4. WHEN 用户设置新密码时，THE Auth_System SHALL 校验新密码长度不少于 8 位且包含字母与数字的组合，不符合则返回错误码 `WEAK_PASSWORD`

---

### 需求 14：接口安全与访问控制白名单

**用户故事：** 作为系统，我希望明确定义公开接口与受保护接口，以便在保证安全的同时允许必要的公开访问。

#### 验收标准

1. THE Auth_System SHALL 将以下接口设为公开（无需认证）：用户注册接口、用户登录接口、验证码获取接口
2. THE Auth_System SHALL 将所有其他接口设为受保护接口，要求携带有效 Access_Token
3. WHEN 未认证用户访问受保护接口时，THE Auth_System SHALL 返回 HTTP 401 状态码，而非重定向至登录页
