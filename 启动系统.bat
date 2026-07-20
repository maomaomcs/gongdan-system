@echo off
chcp 65001 >nul
cd /d "%~dp0"
title 校园报障工单系统 - 启动器

echo ============================================
echo   校园报障工单系统 启动中...
echo ============================================
echo.

echo [1/2] 启动 MySQL 数据库(新窗口,请勿关闭)...
start "MySQL数据库-请勿关闭" /min "%~dp0tooling\mysql-8.0.29-winx64\bin\mysqld.exe" --defaults-file="%~dp0tooling\my.ini" --console
echo     等待数据库就绪...
timeout /t 6 /nobreak >nul

echo [2/2] 启动工单系统后端(新窗口,请勿关闭)...
set "JAVA_HOME=%~dp0tooling\jdk-17.0.19+10"
start "工单系统后端-请勿关闭" "%~dp0tooling\jdk-17.0.19+10\bin\java.exe" -jar "%~dp0backend\target\ticket-0.0.1-SNAPSHOT.jar"
echo     等待后端就绪...
timeout /t 10 /nobreak >nul

echo.
echo ============================================
echo   系统已启动!请在浏览器打开:
echo.
echo   报修入口(老师): http://localhost:8080/
echo   管理后台(你)  : http://localhost:8080/login
echo   管理密码        : admin123
echo ============================================
echo.
echo   提示:关闭本窗口不影响运行。要停止系统,
echo   请关闭"MySQL数据库"和"工单系统后端"两个窗口。
echo.
start http://localhost:8080/
pause
