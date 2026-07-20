@echo off
chcp 65001 >nul
cd /d "%~dp0"
title 校园报障工单系统 - 开发模式

echo 开发模式:前端热更新(改代码即时生效),用于二次开发。
echo.

echo [1/3] 启动 MySQL 数据库...
start "MySQL数据库-请勿关闭" /min "%~dp0tooling\mysql-8.0.29-winx64\bin\mysqld.exe" --defaults-file="%~dp0tooling\my.ini" --console
timeout /t 6 /nobreak >nul

echo [2/3] 启动后端(端口 8080)...
set "JAVA_HOME=%~dp0tooling\jdk-17.0.19+10"
start "工单系统后端-请勿关闭" "%~dp0tooling\jdk-17.0.19+10\bin\java.exe" -jar "%~dp0backend\target\ticket-0.0.1-SNAPSHOT.jar"
timeout /t 8 /nobreak >nul

echo [3/3] 启动前端开发服务器(端口 5173)...
cd /d "%~dp0frontend"
start "前端开发服务器-请勿关闭" cmd /k node node_modules\vite\bin\vite.js --host
timeout /t 4 /nobreak >nul

echo.
echo 开发访问地址: http://localhost:5173/  (改前端代码自动刷新)
start http://localhost:5173/
pause
