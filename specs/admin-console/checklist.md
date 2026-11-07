# 后台管理端验收清单

- [x] 后端新增代码可编译（`mvn -DskipTests compile` exit code 0）
- [x] 前端新增页面可构建（`npm run build` 成功）
- [x] `/api/admin/**` 已注册管理员权限拦截
- [x] 管理员导航入口按角色条件显示
- [x] 重启后端后，管理员可正常访问 `/admin` 四个模块
- [x] 普通用户访问 `/admin` 被重定向至 `/home`
- [x] 删除进行中会议可触发 `END_MEETING` 并清空房间
- [x] 禁用用户后该账号无法登录
