@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
echo ==========================================
echo  安装 KJ 到系统特权 App (/system/priv-app/)
echo ==========================================
echo.

set ADB=D:\SDK\platform-tools\adb.exe
set APK=d:\APK\老電視盒_自動六宮格\app\build\outputs\apk\debug\kj.apk

echo [1/7] 检查 APK 是否存在...
if not exist "%APK%" (
    echo 错误: APK 不存在，请先执行 build打包.bat
    pause
    exit /b 1
)
echo APK 找到: %APK%
echo.

echo [2/7] 扫描局域网寻找电视盒...

:: 获取本机 IP 和子网
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    for /f "tokens=1 delims= " %%b in ("%%a") do (
        set LOCAL_IP=%%b
    )
)

:: 提取子网前缀 (例如 192.168.0)
for /f "tokens=1-3 delims=." %%a in ("%LOCAL_IP%") do (
    set SUBNET=%%a.%%b.%%c
)

echo 本机 IP: %LOCAL_IP%
echo 扫描子网: %SUBNET%.1-254 (先用 ping 快速筛选)
echo.

:: 先用 ping 快速扫描在线主机
set TVBOX_IP=
for /l %%i in (1,1,254) do (
    ping -n 1 -w 500 %SUBNET%.%%i | findstr /c:"TTL" >nul 2>&1
    if !errorlevel! equ 0 (
        :: 主机在线，尝试 ADB 连接
        %ADB% connect %SUBNET%.%%i:5555 >nul 2>&1
        timeout /t 1 /nobreak >nul
        %ADB% devices | findstr /c:"%SUBNET%.%%i:5555" | findstr /c:"device" >nul 2>&1
        if !errorlevel! equ 0 (
            set TVBOX_IP=%SUBNET%.%%i
            echo.
            echo [找到] 电视盒 IP: !TVBOX_IP!
        )
    )
)

if not defined TVBOX_IP (
    echo.
    echo 错误: 未找到电视盒
    echo 请确认:
    echo   1. 电视盒已开机并连接网络
    echo   2. 电视盒 ADB 调试已开启 (端口 5555)
    echo   3. 电脑与电视盒在同一局域网
    pause
    exit /b 1
)

set DEV=%TVBOX_IP%:5555
echo.
echo [3/7] 推送 APK 到电视盒...
%ADB% -s %DEV% push "%APK%" /data/local/tmp/system-app.apk
if !errorlevel! neq 0 (
    echo 错误: APK 推送失败
    pause
    exit /b 1
)

echo [4/7] 重新挂载 /system 为可写...
%ADB% -s %DEV% shell "mount -o remount,rw /system"

echo [5/7] 复制 APK 到 /system/priv-app/...
%ADB% -s %DEV% shell "cp /data/local/tmp/system-app.apk /system/priv-app/tvbox-kj.apk"

echo [6/7] 设置权限...
%ADB% -s %DEV% shell "chmod 644 /system/priv-app/tvbox-kj.apk"
%ADB% -s %DEV% shell "chown system:system /system/priv-app/tvbox-kj.apk"

echo [7/7] 重启电视盒...
%ADB% -s %DEV% reboot

echo.
echo ==========================================
echo  安装完成！电视盒 (%TVBOX_IP%) 正在重启...
echo ==========================================
timeout /t 5 /nobreak
