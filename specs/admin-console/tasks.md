# 后台管理端任务清单

## 阶段一：后端

- [x] **T1 新增管理端 VO**
  - 新建 `AdminStatsVO.java`（7 个 Long 计数）
  - 新建 `AdminUserVO.java`（含 role/enabled/lastLoginAt/createdAt）
  - 新建 `AdminGroupVO.java`（含 ownerName/memberCount）

- [x] **T2 扩展 UserVO**
  - 在 `UserVO.java` 增加 `role`、`enabled`、`lastLoginAt`、`createdAt`
  - 保持既有 `BeanUtils.copyProperties` 映射可用

- [x] **T3 新增 AdminService / AdminServiceImpl**
  - `AdminStatsVO getStats()`
  - `PageResponse<MeetingVO> pageMeetings(int current, int size, String keyword, String status)`
  - `void deleteMeeting(Long meetingId, Long adminUserId)`（含 Netty/Redis/参与者清理）
  - `PageResponse<AdminUserVO> pageUsers(int current, int size, String keyword, Boolean enabled)`
  - `void updateUserStatus(Long userId, Boolean enabled)`
  - `PageResponse<AdminGroupVO> pageGroups(int current, int size, String keyword)`
  - `void deleteGroup(Long groupId)`
  - 依赖：`MeetingMapper`、`MeetingParticipantMapper`、`UserMapper`、`ImGroupMapper`、`ImGroupMemberMapper`、`ImChatStateMapper`、`ChannelContextUtils`、`RedisUtils`、`JwtConfig`
  - 所有列表查询显式 `eq("deleted", false)`

- [x] **T4 新增 AdminController**
  - `@RequestMapping("/api/admin")`
  - 7 个接口（stats / meetings GET+DELETE / users GET+状态 PUT / groups GET+DELETE）
  - 当前管理员 ID 从 `AuthContext.getCurrentUser().getId()` 获取

- [x] **T5 注册管理员拦截器**
  - `WebConfig.addInterceptors` 中 adminInterceptor 增加 `.addPathPatterns("/api/admin/**")`

- [x] **T6 后端编译**
  - `mvn -DskipTests compile` 通过（exit code 0）

## 阶段二：前端

- [x] **T7 API 封装**
  - 新建 `web/src/api/admin.js`，导出 7 个请求方法

- [x] **T8 管理端布局**
  - 新建 `AdminLayout.vue`：浅色侧边栏（数据概览/会议管理/用户管理/群聊管理/返回前台）+ `router-view`

- [x] **T9 四个管理页面**
  - `AdminDashboard.vue`：调用 stats，展示统计卡
  - `AdminMeetings.vue`：远程分页表格 + 关键字/状态筛选 + 删除确认
  - `AdminUsers.vue`：远程分页表格 + 关键字/状态筛选 + 启用禁用开关
  - `AdminGroups.vue`：远程分页表格 + 关键字筛选 + 删除确认

- [x] **T10 路由与入口**
  - `router/index.js` 增加 `/admin` 嵌套路由与 `requiresAdmin` 守卫
  - `MainLayout.vue` 对管理员显示「管理后台」入口

- [x] **T11 前端构建**
  - `web` 目录执行 `npm run build` 通过（built in 14.69s）

## 阶段三：联调验证

- [x] **T12 重启后端并由用户验证**
  - 管理员访问 `/admin`，四个模块功能正常（已通过浏览器验证）
  - 普通用户访问 `/admin` 被重定向 `/home`
  - 删除进行中会议可触发 `END_MEETING`，禁用用户后无法登录
