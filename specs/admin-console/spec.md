# 后台管理端规格说明（Admin Console）

## 1. 目标与范围

在现有 Vue3 + Naive UI + Spring Boot 项目内新增独立后台管理端，路径 `/admin`，复用现有 JWT 登录体系与统一响应结构。

首批模块（均已确认）：

| 模块 | 能力 |
| --- | --- |
| 数据概览 | 用户/会议/群聊核心计数 |
| 会议管理 | 分页查询、关键字/状态筛选、删除会议 |
| 用户管理 | 分页查询、关键字筛选、启用/禁用用户 |
| 群聊管理 | 分页查询、关键字筛选、删除群聊 |

权限：仅 `role = ADMIN` 或 `SUPER_ADMIN` 可访问；不做编辑功能。

## 2. 后端设计

### 2.1 新增文件

| 文件 | 职责 |
| --- | --- |
| `src/main/java/cn/cxdproject/coder/controller/AdminController.java` | `/api/admin/**` 管理接口 |
| `src/main/java/cn/cxdproject/coder/service/AdminService.java` | 管理端服务接口 |
| `src/main/java/cn/cxdproject/coder/service/impl/AdminServiceImpl.java` | 管理端服务实现 |
| `src/main/java/cn/cxdproject/coder/model/vo/AdminStatsVO.java` | 概览统计 VO |
| `src/main/java/cn/cxdproject/coder/model/vo/AdminUserVO.java` | 管理端用户 VO |
| `src/main/java/cn/cxdproject/coder/model/vo/AdminGroupVO.java` | 管理端群聊 VO |

### 2.2 修改文件

| 文件 | 修改点 |
| --- | --- |
| `src/main/java/cn/cxdproject/coder/config/WebConfig.java` | adminInterceptor 增加 `.addPathPatterns("/api/admin/**")` |
| `src/main/java/cn/cxdproject/coder/model/vo/UserVO.java` | 增加 `role`、`enabled`、`lastLoginAt`、`createdAt` 字段（前端路由守卫与用户管理需要） |

### 2.3 接口契约

所有接口返回 `ApiResponse<T>`，分页接口返回 `PageResponse<T>`（`data` 为记录数组，`pagination` 含 `currentPage/pageSize/totalItems/totalPages`）。

| 方法 | 路径 | 参数 | 返回 |
| --- | --- | --- | --- |
| GET | `/api/admin/stats` | 无 | `ApiResponse<AdminStatsVO>` |
| GET | `/api/admin/meetings` | `current=1,size=10,keyword?,status?` | `PageResponse<MeetingVO>` |
| DELETE | `/api/admin/meetings/{id}` | 路径 id | `ApiResponse<Void>` |
| GET | `/api/admin/users` | `current=1,size=10,keyword?,enabled?` | `PageResponse<AdminUserVO>` |
| PUT | `/api/admin/users/{id}/status` | `enabled=true/false` | `ApiResponse<Void>` |
| GET | `/api/admin/groups` | `current=1,size=10,keyword?` | `PageResponse<AdminGroupVO>` |
| DELETE | `/api/admin/groups/{id}` | 路径 id | `ApiResponse<Void>` |

字段定义：

- `AdminStatsVO`：`totalUsers`、`activeUsers`、`disabledUsers`、`totalMeetings`、`ongoingMeetings`、`totalGroups`、`totalGroupMembers`（均为 `Long`）。
- `AdminUserVO`：`id`、`username`、`mobile`、`email`、`avatar`、`role`、`enabled`、`meetingId`、`lastLoginAt`、`createdAt`。
- `AdminGroupVO`：`id`、`groupName`、`groupNo`、`ownerId`、`ownerName`、`memberCount`、`announcement`、`createdAt`。
- 会议列表复用既有 `MeetingVO`，额外填充 `hostUsername`。

### 2.4 业务规则

1. 所有查询必须显式追加 `deleted = false`（MyBatis-Plus 逻辑删除不会自动生效于 QueryWrapper）。
2. 会议列表按 `created_at DESC`；关键字模糊匹配 `topic` 或 `meeting_number`；`status` 精确匹配。
3. 删除会议：
   - 记录不存在或已删除 → `NotFoundException(404, "会议不存在")`。
   - 若状态为 `waiting` 或 `ongoing`：通过 `ChannelContextUtils` 广播 `END_MEETING` 控制消息；将已加入参与者状态置为 `left` 并写入离会时间；清空 Netty 会议室；删除 Redis `meeting:{id}` 与 `meeting:host:{id}`；状态改为 `cancelled`、写入 `actualEndTime`。
   - 最终设置 `deleted = true` 并更新。
4. 用户列表按 `created_at DESC`；关键字匹配 `username`、`mobile`、`email`；`enabled` 可选精确过滤；角色字段直接返回。
5. 启用/禁用用户：用户不存在或已删除 → 404；更新 `status`；禁用时调用 `jwtConfig.invalidateAllUserTokens(username)` 使旧 token 失效。
6. 群聊列表按 `created_at DESC`；关键字匹配 `group_name` 或 `group_no`；批量查询群主用户名与成员数，避免逐行查询。
7. 删除群聊：记录不存在或已删除 → 404；设置 `deleted = true`；物理删除 `im_group_member` 中该群成员；物理删除 `im_chat_state` 中 `conversation_type = 'GROUP' AND target_id = groupId` 的会话状态（与既有解散逻辑保持一致，历史消息保留）。

## 3. 前端设计

### 3.1 新增文件

| 文件 | 职责 |
| --- | --- |
| `web/src/api/admin.js` | 管理端 API 封装 |
| `web/src/views/admin/AdminLayout.vue` | 管理端浅色布局：侧边栏 + 内容区 + 返回前台 |
| `web/src/views/admin/AdminDashboard.vue` | 数据概览卡片 |
| `web/src/views/admin/AdminMeetings.vue` | 会议表格、筛选、删除 |
| `web/src/views/admin/AdminUsers.vue` | 用户表格、筛选、启用/禁用 |
| `web/src/views/admin/AdminGroups.vue` | 群聊表格、筛选、删除 |

### 3.2 修改文件

| 文件 | 修改点 |
| --- | --- |
| `web/src/router/index.js` | 注册 `/admin` 路由（`requiresAuth` + `requiresAdmin`），守卫中校验角色 |
| `web/src/views/layout/MainLayout.vue` | 管理员账号显示「管理后台」导航入口 |

### 3.3 交互规范

- 界面统一浅色（背景 `#f5f7fa`、卡片白色、主色 `#006EFF`）；侧边栏白色，不做深色。
- 表格使用 `n-data-table` 远程分页；每页条数 10，可切换。
- 删除操作使用 `n-dialog` 二次确认，成功后 `useMessage().success()` 提示并刷新列表。
- 用户管理使用 `n-switch` 切换启用状态，切换前确认；禁用需提示「禁用后该用户将无法登录」。
- 概览页四张统计卡：总用户/总会议/总群聊/进行中会议，另展示活跃用户、禁用用户、群成员总数。
- 路由守卫：`requiresAdmin` 时若无 `userInfo` 先 `fetchUserInfo()`，角色不满足则重定向 `/home`。

## 4. 验证步骤

1. 后端：在项目根执行 `mvn -DskipTests compile`，需编译通过。
2. 前端：在 `web` 目录执行 `npm run build`，需构建通过。
3. 重启后端（由用户自行在 IDEA 启动）。
4. 使用管理员账号登录，访问 `http://localhost:3000/admin`：
   - 概览数字正常展示；
   - 会议列表可筛选、可删除，删除进行中会议后前端收到 `END_MEETING`；
   - 用户列表可禁用/启用，禁用后该账号无法登录；
   - 群聊列表可删除，删除后前台会话列表不再出现该群。
5. 普通用户访问 `/admin` 被重定向至 `/home`。

## 5. 审批

以上规格确认后进入实现阶段；实现顺序见 `tasks.md`。
