# 实现计划：基于 Spring Security 的政务管理系统安全认证与权限控制平台

## 概述

按照需求文档与设计文档，将系统拆分为 8 个阶段逐步实现。每个阶段聚焦独立功能模块，阶段间通过检查点确保质量。标注 `*` 的子任务为可选测试任务，可在 MVP 阶段跳过。

---

## 任务列表

- [x] 1. 基础环境与项目骨架
  - [x] 1.1 初始化后端 Spring Boot 项目
    - 创建 Spring Boot 3.x 项目骨架
    - 引入依赖：`spring-boot-starter-web`、`spring-boot-starter-security`、`jjwt`、`redis`、`mybatis`、`mysql`、`lombok`、`validation`
    - 配置 `application.yml`（数据库、Redis、JWT 密钥等）
    - 创建全局异常处理器 `@RestControllerAdvice`，返回统一响应体结构
    - _需求：3、13_

  - [x] 1.2 数据库初始化
    - 创建数据库 `gov_security`
    - 创建基础表：`user`、`role`、`permission`、`dept`、`menu`、`user_role`、`role_permission`、`audit_log`
    - 插入初始角色：`ROLE_CITIZEN`、`ROLE_OFFICER`、`ROLE_SUPER_ADMIN`
    - _需求：6.1、9_

  - [x] 1.3 Redis 配置
    - 配置 Redis 连接池
    - 定义 Redis Key 命名规范常量类（`captcha:`、`access_token:`、`refresh_token:`、`token_blacklist:`、`login_fail:`、`account_lock:`、`user_permissions:`）
    - _需求：2、3、4、5_

- [x] 2. 认证模块开发
  - [x] 2.1 密码加密模块
    - 配置 `BCryptPasswordEncoder`（cost ≥ 10）
    - 编写 `PasswordEncoder` 工具类
    - 配置日志脱敏：确保密码字段不出现在任何日志输出、响应体或数据库明文字段中
    - _需求：1.2、13.1、13.2_

  - [x] 2.2 属性测试：密码加密不可逆且可验证
    - **属性 1：密码加密不可逆且可验证**
    - **验证需求：1.2**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 1`

  - [x] 2.3 图形验证码模块
    - 实现 `CaptchaService`：生成随机字符验证码，Base64 图片输出
    - 将验证码文本以 `captcha:{uuid}` 为 key 存入 Redis，TTL 5 分钟
    - 提供 `GET /api/auth/captcha` 接口，返回 `uuid` 与 Base64 图片
    - _需求：2.1、2.2_

  - [x] 2.4 注册功能开发
    - 实现 `POST /api/auth/register` 接口
    - 校验用户名唯一性（`USER_ALREADY_EXISTS`）
    - 校验手机号格式（`INVALID_PHONE_FORMAT`）
    - 校验密码强度：长度 ≥ 8 且包含字母与数字（`WEAK_PASSWORD`）
    - BCrypt 加密密码后写入数据库，默认分配 `ROLE_CITIZEN`
    - _需求：1.1、1.2、1.3、1.4、1.5、1.6_

  - [x] 2.5 属性测试：弱密码被拒绝
    - **属性 2：弱密码被拒绝**
    - **验证需求：1.5**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 2`

  - [x] 2.6 属性测试：注册成功后默认角色为 ROLE_CITIZEN
    - **属性 3：注册成功后默认角色为 ROLE_CITIZEN**
    - **验证需求：1.6**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 3`

  - [x] 2.7 JWT 模块开发
    - 实现 `TokenManager` 类：生成 Token（HS256，密钥 ≥ 256 位）、解析 Token、校验签名、校验过期时间
    - _需求：3.7_

  - [ ]* 2.8 属性测试：JWT Round-Trip 一致性
    - **属性 4：JWT 生成与解析的 Round-Trip 一致性**
    - **验证需求：3.2**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 4`

  - [x] 2.9 登录功能开发
    - 实现 `POST /api/auth/login` 接口
    - 优先校验图形验证码（`CAPTCHA_INVALID`）
    - 校验账号密码（`INVALID_CREDENTIALS`，不暴露具体原因）
    - 检查账号禁用状态（`ACCOUNT_DISABLED`）
    - 生成 Access_Token（30 分钟）与 Refresh_Token（7 天），写入 Redis
    - 返回 Token + 用户基本信息
    - _需求：2.3、2.4、2.5、2.7、2.8、2.9_

  - [x] 2.10 登录防暴力破解
    - 使用 Redis `login_fail:{username}` 计数，10 分钟窗口内失败 5 次锁定账号 30 分钟
    - 锁定后返回 `ACCOUNT_LOCKED`
    - _需求：2.6_

  - [x] 2.11 属性测试：账号锁定阈值
    - **属性 12：账号锁定阈值**
    - **验证需求：2.6**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 12`

  - [x] 2.12 JWT_Filter 实现
    - 继承 `OncePerRequestFilter`，提取 `Authorization: Bearer <token>`
    - 依次校验：签名有效性、过期时间、Redis 黑名单
    - 校验通过后将用户信息写入 `SecurityContextHolder`
    - 返回对应错误码：`TOKEN_MISSING`、`TOKEN_INVALID`、`TOKEN_EXPIRED`、`TOKEN_REVOKED`
    - _需求：3.1、3.2、3.3、3.4、3.5、3.6_

  - [x] 2.13 Token 刷新模块
    - 实现 `POST /api/auth/refresh` 接口
    - 校验 Refresh_Token 有效性，使用后立即删除旧 Refresh_Token（一次性机制）
    - 生成新 Access_Token 并更新 Redis
    - 无效时返回 `REFRESH_TOKEN_INVALID`
    - _需求：4.1、4.2、4.3、4.4_

  - [ ]* 2.14 属性测试：Refresh_Token 一次性使用
    - **属性 8：Refresh_Token 一次性使用**
    - **验证需求：4.4**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 8`

  - [x] 2.15 登出功能
    - 实现 `POST /api/auth/logout` 接口
    - 将当前 Access_Token 的 jti 加入 Redis 黑名单 `token_blacklist:{jti}`，TTL 与 Token 剩余有效期一致
    - 删除 Redis 中对应的 Refresh_Token
    - _需求：5.1、5.2、5.3_

  - [ ]* 2.16 属性测试：登出后 Token 被加入黑名单
    - **属性 5：登出后 Token 被加入黑名单**
    - **验证需求：5.1**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 5`

  - [x] 2.17 密码修改功能
    - 实现密码修改接口
    - 要求用户提供当前密码进行验证，失败返回 `CURRENT_PASSWORD_WRONG`
    - 校验新密码强度（`WEAK_PASSWORD`）
    - BCrypt 加密新密码后更新数据库，使该用户所有在线 Token 立即失效
    - 记录审计日志（密码修改操作）
    - _需求：13.3、13.4、10.1_

- [x] 3. 检查点 - 认证模块验证
  - 确保所有认证相关测试通过，验证登录、登出、Token 刷新完整流程，如有问题请向用户反馈。

- [x] 4. 权限系统开发
  - [x] 4.1 RBAC 权限模型实现
    - 创建实体类：`User`、`Role`、`Permission`、`Dept`
    - 实现 `UserDetails` 接口，将用户角色与权限注入 `GrantedAuthority`
    - 实现 `UserDetailsService`，从数据库加载用户信息
    - _需求：6.1、6.2_

  - [x] 4.2 方法级权限控制
    - 启用 `@EnableMethodSecurity`
    - 在 Controller/Service 层使用 `@PreAuthorize("hasAuthority('xxx')")` 声明权限
    - 无权限时返回 HTTP 403 及 `ACCESS_DENIED`
    - _需求：6.3、6.4_

  - [x] 4.3 属性测试：无权限访问返回 403
    - **属性 6：无权限访问返回 403**
    - **验证需求：6.3**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 6`

  - [x] 4.4 权限缓存
    - 登录时将用户权限列表缓存至 Redis `user_permissions:{userId}`，TTL 30 分钟
    - 角色变更时立即清除对应用户的权限缓存
    - _需求：6.5_

  - [x] 4.5 数据权限拦截器
    - 实现 MyBatis `DataScopeInterceptor`，动态拼接 SQL WHERE 条件
    - 支持三种数据权限范围：`ALL`（不过滤）、`DEPT`（按 dept_id 过滤）、`SELF`（按 create_by 过滤）
    - _需求：7.1、7.2、7.3、7.4_

  - [ ]* 4.6 属性测试：数据权限过滤正确性
    - **属性 7：数据权限过滤正确性**
    - **验证需求：7.2、7.3**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 7`

  - [x] 4.7 接口安全与访问控制白名单配置
    - 在 Spring Security 配置中将以下接口设为公开（`permitAll`）：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/captcha`
    - 将所有其他接口设为受保护，要求携带有效 Access_Token
    - 配置未认证访问受保护接口时返回 HTTP 401（禁止重定向至登录页）
    - _需求：14.1、14.2、14.3_

- [x] 5. 安全增强模块
  - [x] 5.1 XSS_Filter 实现
    - 继承 `HttpServletRequestWrapper`，对请求参数与请求体中的 HTML 特殊字符（`<`、`>`、`"`、`'`、`&`）进行转义
    - 当请求体为 JSON 时，递归处理所有字符串字段
    - 在响应头中设置 `X-XSS-Protection: 1; mode=block` 及 `Content-Security-Policy`
    - _需求：12.1、12.2、12.3_

  - [x] 5.2 属性测试：XSS 过滤转义完整性
    - **属性 10：XSS 过滤转义完整性**
    - **验证需求：12.1**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 10`

  - [ ]* 5.3 属性测试：JSON 递归 XSS 过滤
    - **属性 11：JSON 递归 XSS 过滤**
    - **验证需求：12.2**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 11`

  - [x] 5.4 CSRF 防护配置
    - 在 Spring Security 配置中，基于 JWT 无状态认证模式禁用传统 Session CSRF Token
    - 通过 JWT_Filter 强制所有状态变更请求（POST、PUT、DELETE）携带有效 `Authorization` Token，以此替代传统 CSRF Token 机制
    - 配置 CORS 策略，仅允许白名单域名发起跨域请求
    - _需求：11.1、11.2、11.3_

  - [x] 5.5 审计日志模块
    - 实现 AOP 切面 `AuditLogAspect`，拦截关键操作：登录、登出、登录失败、密码修改、角色变更、权限变更、用户禁用/启用
    - 记录字段：操作人 ID、操作类型、操作时间、客户端 IP、操作结果、变更前后数据快照（JSON）
    - 使用线程池异步写入 `audit_log` 表，不阻塞主业务流程
    - 权限变更时单独记录权限变更记录，包含变更前角色列表与变更后角色列表
    - _需求：10.1、10.2、10.3、10.5_

  - [x] 5.6 属性测试：审计日志字段完整性
    - **属性 9：审计日志字段完整性**
    - **验证需求：10.2**
    - 使用 jqwik，迭代 ≥ 100 次，标注 `// Feature: gov-security-auth-system, Property 9`

  - [x] 5.7 审计日志查询接口
    - 实现 `GET /api/admin/audit-logs` 接口，支持按操作人、操作类型、时间范围分页查询
    - 仅 `ROLE_SUPER_ADMIN` 可访问
    - _需求：10.4、10.5_

- [x] 6. 检查点 - 安全模块验证
  - 确保所有安全相关测试通过，验证 XSS 过滤、CSRF 防护、审计日志完整流程，如有问题请向用户反馈。

- [x] 7. 后台管理模块
  - [x] 7.1 用户管理模块
    - 实现 `GET /api/admin/users` 分页查询接口（支持按用户名、角色、状态过滤）
    - 实现 `POST /api/admin/users` 创建用户接口（指定用户名、密码、角色、部门）
    - 实现 `PUT /api/admin/users/{id}/status` 禁用/启用用户接口，禁用时使该用户所有在线 Token 立即失效
    - 实现 `PUT /api/admin/users/{id}/password` 重置密码接口，BCrypt 加密后更新，同时使该用户所有在线 Token 失效
    - _需求：9.1、9.2、9.3、9.4_

  - [x] 7.2 角色管理模块
    - 实现角色 CRUD 接口：`GET/POST /api/admin/roles`、`PUT/DELETE /api/admin/roles/{id}`
    - 实现 `PUT /api/admin/roles/{id}/permissions` 为角色分配权限
    - 删除角色时校验是否有关联用户，若有则返回 `ROLE_IN_USE`
    - _需求：9.5、9.6_

  - [x] 7.3 菜单与路由接口
    - 实现 `GET /api/route/menus` 接口，根据用户角色与权限返回菜单树及按钮权限标识列表
    - _需求：8.1_

- [x] 8. 前端开发
  - [x] 8.1 登录页面
    - 实现验证码展示（调用 `GET /api/auth/captcha`，展示 Base64 图片）
    - 实现登录表单（用户名、密码、验证码）
    - 登录成功后将 Access_Token 与 Refresh_Token 存储至 `localStorage`
    - _需求：2_

  - [x] 8.2 Axios 拦截器
    - 请求拦截器：自动从 `localStorage` 读取 Access_Token 并附加 `Authorization: Bearer <token>` 头
    - 响应拦截器：检测到 HTTP 401 且错误码为 `TOKEN_EXPIRED` 时，自动调用 `POST /api/auth/refresh` 刷新 Token，刷新成功后重试原请求；刷新失败则清除本地 Token 并跳转登录页
    - _需求：4.5_

  - [x] 8.3 动态路由
    - 登录成功后调用 `GET /api/route/menus`，根据返回的菜单列表通过 `router.addRoute()` 动态注册路由
    - 未授权路由访问时重定向至 403 页面
    - _需求：8.1、8.2、8.3_

  - [x] 8.4 按钮权限控制
    - 实现 `v-permission` 自定义指令，根据用户权限列表控制按钮的显示与隐藏
    - 用户权限发生变更后，下次登录时重新调用 `GET /api/route/menus` 刷新路由与按钮权限
    - _需求：8.4、8.5_

  - [x] 8.5 注册页面
    - 实现注册表单（用户名、密码、手机号、真实姓名）
    - 前端校验密码强度与手机号格式，提供实时反馈
    - 调用 `POST /api/auth/register`，处理各类错误码提示（`USER_ALREADY_EXISTS`、`INVALID_PHONE_FORMAT`、`WEAK_PASSWORD`）
    - _需求：1_

- [x] 9. 测试
  - [x] 9.1 单元测试
    - 密码加密与验证测试
    - 登录完整流程测试（含验证码优先校验）
    - Token 刷新流程测试
    - 权限校验测试
    - _需求：1、2、3、4、6_

  - [ ]* 9.2 属性测试全覆盖（jqwik）
    - 确认属性 1～12 均有对应属性测试，每个测试 ≥ 100 次迭代
    - 每个测试标注对应属性编号与需求编号
    - _需求：1～12_

  - [ ]* 9.3 集成测试
    - JWT_Filter 端到端过滤链测试
    - 数据权限拦截器 SQL 拼接验证
    - Redis 黑名单机制验证
    - _需求：3、5、7_

- [x] 10. 检查点 - 最终验证
  - 确保所有测试通过，验证完整业务流程（注册→登录→鉴权→登出），如有问题请向用户反馈。

- [x] 11. 优化与完善
  - [x] 11.1 性能优化
    - Redis 缓存策略优化（权限缓存 TTL 调整）
    - SQL 索引优化（`username`、`dept_id`、`create_by` 等高频查询字段）
    - 分页查询性能优化
    - _需求：7、9_

  - [x] 11.2 安全优化
    - CORS 白名单域名配置完善
    - 强密码提示优化（前端实时反馈）
    - Token 结构优化（Payload 字段精简）
    - _需求：11.3、13_

---

## 备注

- 标注 `*` 的子任务为可选测试任务，MVP 阶段可跳过
- 每个任务均引用对应需求编号，确保需求可追溯
- 检查点任务确保阶段性质量验证
- 属性测试验证系统通用正确性，单元测试验证具体示例与边界条件
