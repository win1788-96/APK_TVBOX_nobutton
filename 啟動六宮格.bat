@echo off
chcp 65001 >nul
echo ==========================================
echo   Bingo TV - 一鍵啟動六宮格
echo ==========================================
echo.

set ADB=D:\SDK\platform-tools\adb.exe
set DEV=192.168.0.23:5555

echo [1/3] 連接電視盒...
%ADB% connect 192.168.0.23 >nul 2>&1
timeout /t 2 /nobreak >nul

echo [2/3] 啟動 APP 並自動切換六宮格...
%ADB% -s %DEV% shell "am force-stop tvbox.kj" >nul 2>&1
%ADB% -s %DEV% shell "am start -n tvbox.kj/tvbox.kj.MainActivity; sleep 15; input tap 1080 18"

echo [3/3] 等待頁面載入 + 切換完成...
timeout /t 18 /nobreak >nul

echo.
echo ==========================================
echo   完成！畫面已切換到六宮格布局
echo ==========================================
pause
