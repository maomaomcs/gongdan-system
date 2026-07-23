# 石室联中132 · 后勤报修（老师端小程序）

微信**原生小程序**，复用现有工单系统后端（Spring Boot，端口 8082）。老师微信里报修、查进度、催单、取消；管理端仍用网页后台。

## 目录
```
miniprogram/
  app.js / app.json / app.wxss     全局
  config.js                        ★后端地址、开关(常改这里)
  sitemap.json / project.config.json
  utils/api.js                     请求封装(带 token / 上传 / 拼完整图片地址)
  pages/
    login/     登录 + 注册(邀请码)
    report/    我要报修(类型/位置/描述/紧急度/拍照)
    my/        我的报修(筛选/分页/红点/催N/超时)
    detail/    报修详情(进展时间线 + 催单/取消)
```

## 如何运行（开发调试）
1. 装**微信开发者工具**，登录你自己的微信账号。
2. 打开工具 → 导入项目 → 目录选 `ticket-system-pro/miniprogram`。
3. **AppID**：填你自己的小程序 AppID（在 `project.config.json` 里现在是占位的 `touristappid`，导入时改成你的；用测试号也能先跑账号登录，但微信登录/发布需要真实 AppID）。
4. 工具右上「详情 → 本地设置」勾选 **不校验合法域名**（这样能连当前 `http://43.136.56.131:8082` 后端联调）。
5. 编译预览：默认进「我要报修」，未登录会跳登录页。用后台生成的**邀请码**注册老师账号，或用已有老师账号登录。

## 后端地址在哪改
`config.js` 的 `apiBase`：
- 开发：`http://43.136.56.131:8082`（需勾"不校验合法域名"）
- 上线：改成**已备案的 https 域名**（如 `https://bx.mm1.asia`），并在小程序后台「开发管理 → 服务器域名」把它加进 **request 合法域名** 和 **uploadFile 合法域名**。

## 复用的后端接口（无需改动即可用）
- `POST /api/user/login`、`POST /api/user/register`（注册需 inviteCode）
- `GET /api/config`（故障类型/位置/FAQ/紧急度）
- `POST /api/user/tickets`（提交报修，字段 reporter/location/category/title/description/urgency/images）
- `GET /api/user/tickets?page=&size=&status=`（我的报修，分页）
- `GET /api/user/tickets/{id}`、`POST .../urge`、`POST .../cancel`
- `POST /api/upload`（图片，返回 `{name,url}`）、`GET /api/files/{name}`

## 上线前置条件（需你/学校办理）
1. **HTTPS 域名**：把后端反代到 `https://bx.mm1.asia`（宝塔反代 8082 + Let's Encrypt 证书）。小程序不允许请求 http/IP。
2. **服务器域名白名单**：小程序后台配置 request/uploadFile 合法域名。
3. **小程序备案**：微信侧单独备案（域名备案≠小程序备案）。
4. 提交审核 → 发布。

## 可选增强（后端待做，需要时再加）
- **微信一键登录**：新增 `POST /api/wx/login`——小程序 `wx.login` 拿 code，后端用 AppID/AppSecret 调 `code2session` 换 openid，关联/创建老师账号后发登录令牌；老师免账号密码。做好后把 `config.js` 的 `wxLoginEnabled` 置 true 并在登录页加"微信登录"按钮。
- **订阅消息**：工单状态变更时给报修老师推送微信通知（替代/补充钉钉）。

## 说明
- 请求不受浏览器 CORS 限制（`wx.request` 无 Origin），后端现有配置即可。
- 令牌存 `wx.getStorageSync('user_token')`，401 自动回登录页。
- 图片地址后端返回相对路径 `/api/files/xxx`，前端用 `api.fullUrl()` 拼成完整地址显示。
