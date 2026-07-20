# 校园报障工单系统(企业版)

正规企业级技术栈搭建的设备报修/工单管理系统。老师扫码报修,管理员在后台跟进处理,并自动生成统计图表(哪类问题最多、哪间教室最坏、月度趋势)。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + Element Plus + ECharts + Vue Router + Axios |
| 后端 | Spring Boot 3.4 + Spring Data JPA + Spring Validation |
| 数据库 | MySQL 8.0 |
| 运行环境 | 便携 JDK 17(随项目自带,不污染系统) |

前端打包后由后端一起托管,**生产环境只需一个服务(端口 8080)**。

## 目录结构

```
ticket-system-pro/
├── 启动系统.bat        ← 双击这个启动整个系统
├── 重新构建.bat        ← 改过代码后重新打包
├── 开发模式.bat        ← 前端热更新开发模式
├── backend/            Spring Boot 后端
│   ├── src/main/java/com/school/ticket/
│   │   ├── entity/         实体(Ticket, TicketLog)
│   │   ├── repository/     JPA 仓库
│   │   ├── service/        业务逻辑
│   │   ├── controller/     REST 接口(公开 + 后台)
│   │   ├── dto/            请求/响应对象
│   │   ├── web/            鉴权拦截器、异常处理、SPA 托管
│   │   └── config/         配置
│   └── src/main/resources/
│       ├── application.yml 配置(数据库、密码、故障类型)
│       └── static/         前端打包产物(自动生成)
├── frontend/           Vue 3 前端源码
│   └── src/
│       ├── views/          页面(报修/查询/登录/工单管理/统计)
│       ├── layouts/        后台布局
│       ├── api/            接口封装
│       └── router/         路由
└── tooling/            便携 JDK + 便携 MySQL(自带,勿删)
    ├── jdk-17.0.19+10/
    ├── mysql-8.0.29-winx64/
    ├── mysql-data/         数据库数据(备份时复制这个)
    └── my.ini              MySQL 配置
```

## 快速开始

**双击 `启动系统.bat`**,会自动:
1. 启动 MySQL 数据库(独立窗口)
2. 启动后端 + 前端(独立窗口)
3. 打开浏览器

然后访问:

- 报修入口(老师):http://localhost:8080/
- 管理后台(你)  :http://localhost:8080/login  — 默认密码 `admin123`

停止系统:关闭弹出的"MySQL数据库"和"工单系统后端"两个窗口即可。

## 让老师扫码报修

1. 查本机局域网 IP:命令行运行 `ipconfig`,找到 IPv4 地址,如 `192.168.1.50`。
2. 报修入口就是 `http://192.168.1.50:8080/`。
3. 用任意"网址转二维码"工具把这个地址生成二维码,打印贴在各教室。
4. 老师用手机(连同一 Wi-Fi)扫码 → 填单 → 提交。

## 修改配置

编辑 `backend/src/main/resources/application.yml`:

- `app.admin-password` — 管理后台密码(**请务必修改**)
- `app.categories` — 故障类型下拉选项
- `spring.datasource.*` — 数据库连接(默认连本地自带 MySQL)

也可用环境变量覆盖:`ADMIN_PASSWORD`、`DB_USER`、`DB_PASSWORD`、`PORT`(改端口需同时改端口相关脚本)。

改完 `application.yml` 只需重启后端;改了前端/后端代码需运行 `重新构建.bat`。

## 数据备份

数据都在 MySQL 里。备份方式二选一:

- 直接复制 `tooling/mysql-data/` 整个文件夹(需先停止 MySQL)。
- 或用 `tooling/mysql-8.0.29-winx64/bin/mysqldump.exe -u root ticket_system > 备份.sql`。

## 接口一览

公开接口:
- `POST /api/tickets` 提交报修
- `GET  /api/tickets/code/{code}` 按工单号查进度
- `GET  /api/config` 下拉配置
- `POST /api/login` 管理员登录(返回 token)

后台接口(需 `Authorization: Bearer <token>`):
- `GET   /api/admin/tickets` 工单列表(支持 status/category/urgency/location/q 筛选)
- `GET   /api/admin/tickets/{id}` 详情
- `PATCH /api/admin/tickets/{id}` 更新状态/处理人/解决方案
- `POST  /api/admin/tickets/{id}/logs` 添加跟进记录
- `GET   /api/admin/stats` 统计数据

## 说明

- 便携 JDK 与 MySQL 均随项目自带,无需在系统里安装任何东西,删除整个文件夹即完全卸载。
- 登录 token 存于后端内存,后端重启需重新登录(内部系统足够)。
