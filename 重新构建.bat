@echo off
chcp 65001 >nul
cd /d "%~dp0"
title 校园报障工单系统 - 重新构建

echo 修改过代码后运行本脚本,重新打包前端 + 后端。
echo.

echo [1/2] 构建前端(Vue)...
cd /d "%~dp0frontend"
call node node_modules\vite\bin\vite.js build
if errorlevel 1 ( echo 前端构建失败! & pause & exit /b 1 )

echo.
echo [2/2] 构建后端(Spring Boot)...
set "JAVA_HOME=%~dp0tooling\jdk-17.0.19+10"
cd /d "%~dp0backend"
call mvnw.cmd -s settings.xml -DskipTests clean package
if errorlevel 1 ( echo 后端构建失败! & pause & exit /b 1 )

echo.
echo ============================================
echo   构建完成!运行 启动系统.bat 即可。
echo ============================================
pause
