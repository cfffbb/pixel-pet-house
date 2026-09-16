@echo off
chcp 65001 >nul
title AI 宠物助手 - 一键启动(前后端融合)
cd /d %~dp0

echo ==========================================
echo    AI 宠物助手 - 一键启动
echo ==========================================

rem ---- 1. 数据库密码(安全红线:占位符,请改成你本机 MySQL 密码) ----
rem    也可以提前建 pet-backend\.env.local,里面只写密码(一行),脚本自动读取
set "DB_PASS=<你的密钥>"
if exist "pet-backend\.env.local" set /p DB_PASS=<"pet-backend\.env.local"
if "%DB_PASS%"=="<你的密钥>" (
  echo [!] 警告:数据库密码还是占位符,请编辑本脚本的 DB_PASS 或创建 pet-backend\.env.local
)

rem ---- 2. 后端(已在运行则跳过) ----
curl -s -o nul -w "%%{http_code}" http://localhost:8080/api/health > "%TEMP%\pet_h1.txt" 2>nul
set /p H1=<"%TEMP%\pet_h1.txt"
if "%H1%"=="200" (
  echo [ok] 后端已在运行,跳过
) else (
  echo [1/2] 启动后端...
  start "pet-backend" cmd /c "cd /d %~dp0pet-backend && set SPRING_DATASOURCE_PASSWORD=%DB_PASS%&& ..\.tools\apache-maven-3.9.16\bin\mvn.cmd -B -s ..\.tools\maven-settings.xml spring-boot:run"
  echo       等待后端就绪(首次 10-30 秒)...
  :wait
  timeout /t 2 /nobreak >nul
  curl -s -o nul -w "%%{http_code}" http://localhost:8080/api/health > "%TEMP%\pet_h2.txt" 2>nul
  set /p H2=<"%TEMP%\pet_h2.txt"
  if not "%H2%"=="200" goto wait
  echo [ok] 后端就绪
)

rem ---- 3. 前端(Electron + Vite) ----
echo [2/2] 启动客户端...
cd /d %~dp0pet-client
start "pet-client" cmd /c "npm run dev"
echo [ok] 已启动:稍后弹出宠物助手窗口;关窗口=缩到托盘,托盘图标可退出
pause
