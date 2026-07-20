@echo off
chcp 65001 >nul
cd /d "%~dp0"
title 校园报障工单系统 - 一键部署

echo ================================================================
echo   校园报障工单系统 · 一键部署
echo   适用于:已安装 JDK 17、Node.js、MySQL 的电脑(如从 GitHub 克隆)
echo ================================================================
echo.

REM ---------- 环境自检 ----------
echo [检查环境]
set MISSING=0

where java >nul 2>nul
if errorlevel 1 ( echo   [X] 未找到 Java,请先安装 JDK 17 & set MISSING=1 ) else ( echo   [√] Java 已安装 )

where node >nul 2>nul
if errorlevel 1 ( echo   [X] 未找到 Node.js,请先安装 Node.js 18+ & set MISSING=1 ) else ( echo   [√] Node.js 已安装 )

where npm >nul 2>nul
if errorlevel 1 ( echo   [X] 未找到 npm & set MISSING=1 ) else ( echo   [√] npm 已安装 )

if "%MISSING%"=="1" (
  echo.
  echo 缺少必要环境,请安装后重试。MySQL 也需自行安装并启动。
  echo 若本机没有这些环境,建议改用"方案A:直接拷带 tooling 的文件夹"。
  pause
  exit /b 1
)
echo.

REM ---------- 数据库提示 ----------
echo [数据库] 默认连接 localhost:3306,用户 root,空密码,库名 ticket_system(自动创建)。
echo          如与你的 MySQL 不同,请先设置环境变量,例如:
echo            set DB_USER=root
echo            set DB_PASSWORD=你的密码
echo          或直接编辑 backend\src\main\resources\application.yml
echo.
echo 按任意键开始构建(Ctrl+C 取消)...
pause >nul

REM ---------- 1. 构建前端 ----------
echo.
echo [1/3] 安装前端依赖并打包...
cd /d "%~dp0frontend"
call npm install
if errorlevel 1 ( echo 前端依赖安装失败! & pause & exit /b 1 )
call npm run build
if errorlevel 1 ( echo 前端打包失败! & pause & exit /b 1 )

REM ---------- 2. 构建后端 ----------
echo.
echo [2/3] 构建后端(首次会下载依赖,请耐心等待)...
cd /d "%~dp0backend"
call mvnw.cmd -DskipTests clean package
if errorlevel 1 ( echo 后端构建失败! & pause & exit /b 1 )

REM ---------- 3. 启动 ----------
echo.
echo [3/3] 启动系统...
for /f "delims=" %%i in ('dir /b /s target\*.jar ^| findstr /v ".original"') do set JARFILE=%%i
start "工单系统后端-请勿关闭" java -jar "%JARFILE%"
timeout /t 10 /nobreak >nul

echo.
echo ================================================================
echo   部署完成!浏览器打开:
echo   报修入口: http://localhost:8080/
echo   管理后台: http://localhost:8080/login  (初始账号 admin / admin123)
echo ================================================================
start http://localhost:8080/
pause
