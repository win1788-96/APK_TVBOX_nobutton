@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
echo ==========================================
echo  安裝 KJ 到系統特權 App (/system/priv-app/)
echo ==========================================
echo.

set ADB=D:\SDK\platform-tools\adb.exe
set APK=d:\APK\老電視盒_自動六宮格\app\build\outputs\apk\debug\kj.apk

echo [1/7] 檢查 APK 是否存在...
if not exist "%APK%" (
    echo 錯誤: APK 不存在，請先執行 build打包.bat
    pause
    exit /b 1
)
echo APK 找到: %APK%
echo.

echo [2/7] 掃描區域網路尋找電視盒...

:: 獲取本機 IP 和子網
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    for /f "tokens=1 delims= " %%b in ("%%a") do (
        set LOCAL_IP=%%b
    )
)

:: 提取子網前綴 (例如 192.168.0)
for /f "tokens=1-3 delims=." %%a in ("%LOCAL_IP%") do (
    set SUBNET=%%a.%%b.%%c
)

echo 本機 IP: %LOCAL_IP%
echo 掃描子網: %SUBNET%.1-254 (先用 ping 快速篩選)
echo.

:: 先用 ping 快速掃描在線主機
set TVBOX_IP=
for /l %%i in (1,1,254) do (
    ping -n 1 -w 500 %SUBNET%.%%i | findstr /c:"TTL" >nul 2>&1
    if !errorlevel! equ 0 (
        :: 主機在線，嘗試 ADB 連接
        %ADB% connect %SUBNET%.%%i:5555 >nul 2>&1
        timeout /t 1 /nobreak >nul
        %ADB% devices | findstr /c:"%SUBNET%.%%i:5555" | findstr /c:"device" >nul 2>&1
        if !errorlevel! equ 0 (
            set TVBOX_IP=%SUBNET%.%%i
            echo.
            echo [找到] 電視盒 IP: !TVBOX_IP!
        )
    )
)

if not defined TVBOX_IP (
    echo.
    echo 錯誤: 未找到電視盒
    echo 請確認:
    echo   1. 電視盒已開機並連接網路
    echo   2. 電視盒 ADB 偵錯已開啟 (端口 5555)
    echo   3. 電腦與電視盒在同一區域網路
    pause
    exit /b 1
)

set DEV=%TVBOX_IP%:5555
echo.
echo [3/7] 推送 APK 到電視盒...
%ADB% -s %DEV% push "%APK%" /data/local/tmp/system-app.apk
if !errorlevel! neq 0 (
    echo 錯誤: APK 推送失敗
    pause
    exit /b 1
)

echo [4/7] 重新掛載 /system 為可寫...
%ADB% -s %DEV% shell "mount -o remount,rw /system"

echo [5/7] 複製 APK 到 /system/priv-app/...
%ADB% -s %DEV% shell "cp /data/local/tmp/system-app.apk /system/priv-app/tvbox-kj.apk"

echo [6/7] 設定權限...
%ADB% -s %DEV% shell "chmod 644 /system/priv-app/tvbox-kj.apk"
%ADB% -s %DEV% shell "chown system:system /system/priv-app/tvbox-kj.apk"

echo [7/7] 重啟電視盒...
%ADB% -s %DEV% reboot

echo.
echo ==========================================
echo  安裝完成！電視盒 (%TVBOX_IP%) 正在重啟...
echo ==========================================
timeout /t 5 /nobreak
