# 老電視盒 KJ - 專案 WIKI

## 專案概述

專為 Android 4.4 (KitKat) 舊型電視盒優化的 IPTV 網頁導航應用程式。  
內建騰訊 TBS (X5 Core) 瀏覽器核心，解決舊型電視盒 WebView 白屏與 SSL 錯誤問題。

### 兩個版本

| 資料夾 | 說明 |
|--------|------|
| `老電視盒` | 原始版本，無自動點擊功能 |
| `老電視盒_自動六宮格` | 自動切換六宮格版本（展示用） |

---

## 執行環境

| 項目 | 規格 |
|------|------|
| 設備 | HiSTB 老電視盒 (展示用) |
| Android 版本 | 4.4 (API 19)，實際報告 11.0.1 (偽造) |
| 螢幕解析度 | 1280x720 |
| 瀏覽核心 | 騰訊 TBS X5 (com.tencent.smtt.sdk) |
| 開發語言 | Kotlin |
| JDK | D:\java\jdk-17.0.2 |
| ADB | D:\SDK\platform-tools\adb.exe |
| 最低 SDK | 19 (Android 4.4) |
| 目標 SDK | 34 (Android 14) |

---

## 網址列表

| 包名 | 網址 |
|------|------|
| tvbox.bingo | https://lotto.auzo.tw/tv/bingoTv/index.php |
| tvbox.kuaishou | https://bingo.kuaishou1688.com/ |
| tvbox.kj | https://unabrasively-clothlike-lynn.ngrok-free.dev/tvbox |
| tvbox.combotv | https://lotto.auzonet.com/tv/comboTv/ |

---

## 自動六宮格版本

### 背景

網頁使用 **Laya 遊戲引擎** (Canvas 渲染)，按鈕不是 DOM 元素，無法用 JavaScript 選擇器點擊。  
必須使用**內核級觸摸注入**模擬物理觸控。

### 宮格循環模式

```
2格 → 點擊 → 4格 → 點擊 → 6格 → 點擊 → 2格 (循環)
```

從 2 格到 6 格需要點擊 **2 次**。

### 自動切換流程

```
1. 用戶打開 APK
2. MainActivity 載入 → 選擇頻道 → 開啟 WebViewActivity
3. WebView 載入網頁
4. onPageFinished 觸發
5. 等待 3 秒 → 關閉 SweetAlert2 彈窗 (JS 注入)
6. 等待 15 秒 → Laya 引擎完全初始化
7. 第 1 次點擊 (1080, 18) → 2格 → 4格
8. 等待 3 秒
9. 第 2 次點擊 (1080, 18) → 4格 → 6格
10. 每小時自動刷新頁面並重新執行上述流程
```

### 觸摸注入原理

使用 `InputManager.injectInputEvent()` 反射調用（隱藏 API）：

```kotlin
val imClass = Class.forName("android.hardware.input.InputManager")
val im = imClass.getMethod("getInstance").invoke(null)
val injectMethod = im.javaClass.methods.find { it.name == "injectInputEvent" }

// 構造 MotionEvent 並注入
val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
down.source = InputDevice.SOURCE_TOUCHSCREEN
injectMethod.invoke(im, down, 2) // 2 = INJECT_MODE_WAIT
```

此方法等同於 `adb input tap`，是**內核級注入**，X5 WebView 的 Laya Canvas 可以識別。

### 必要權限

| 權限 | 說明 |
|------|------|
| `INJECT_EVENTS` | 注入觸摸事件（簽名級權限，僅系統 App 可獲得） |

### 安裝位置

APK 必須安裝到 `/system/priv-app/`（特權系統 App 目錄）：

```
/system/priv-app/tvbox-kj.apk
├── 權限: 644 (rw-r--r--)
├── 擁有者: system:system
└── 重啟後自動載入
```

> **注意**：`/system/app/` 不夠權限，系統會拒絕授予 `INJECT_EVENTS`。  
> 必須放到 `/system/priv-app/` 才能獲得 `PRIVILEGED` 標誌。

---

## 必須環境

### 電腦端

| 工具 | 路徑 | 用途 |
|------|------|------|
| JDK 17 | `D:\java\jdk-17.0.2` | 編譯 Kotlin/Android |
| Android SDK | `D:\SDK` | platform-tools (adb) |
| ADB | `D:\SDK\platform-tools\adb.exe` | 推送 APK、連接電視盒 |
| Gradle | 專案內建 (`gradlew.bat`) | 編譯 APK |

### 電視盒端

| 項目 | 說明 |
|------|------|
| Root 權限 | 必須，才能寫入 `/system/` |
| ADB 偵錯 | 必須開啟，端口 5555 |
| 網路 | 與電腦同區網 |

---

## 建置與部署

### 步驟 1：編譯 APK

```
雙擊「build打包.bat」
```

或手動執行：
```batch
set JAVA_HOME=D:\java\jdk-17.0.2
cd d:\APK\老電視盒_自動六宮格
gradlew assembleDebug
```

輸出：`app\build\outputs\apk\debug\kj.apk`

### 步驟 2：安裝到系統

```
雙擊「安裝系統App.bat」
```

腳本會自動：
1. 掃描區域網路尋找電視盒（ping + ADB connect）
2. 推送 APK 到 `/data/local/tmp/`
3. 重新掛載 `/system` 為可寫
4. 複製到 `/system/priv-app/tvbox-kj.apk`
5. 設定權限 (chmod 644, chown system:system)
6. 重啟電視盒

### 手動安裝命令

```batch
set ADB=D:\SDK\platform-tools\adb.exe
set DEV=192.168.0.23:5555

%ADB% connect %DEV%
%ADB% push kj.apk /data/local/tmp/system-app.apk
%ADB% shell "mount -o remount,rw /system"
%ADB% shell "cp /data/local/tmp/system-app.apk /system/priv-app/tvbox-kj.apk"
%ADB% shell "chmod 644 /system/priv-app/tvbox-kj.apk"
%ADB% shell "chown system:system /system/priv-app/tvbox-kj.apk"
%ADB% reboot
```

---

## 專案結構

```
老電視盒_自動六宮格/
├── app/
│   └── src/main/
│       ├── java/tvbox/kj/
│       │   ├── MainActivity.kt        ← CSV 解析 + GridView 選單
│       │   ├── WebViewActivity.kt     ← X5 WebView + 自動點擊注入
│       │   ├── RotationManager.kt     ← 螢幕旋轉控制
│       │   └── ClickService.kt        ← 無障礙服務 (API 24+ 備用)
│       ├── res/
│       │   ├── layout/                ← XML 佈局檔
│       │   └── xml/
│       │       └── click_service_config.xml
│       └── AndroidManifest.xml        ← 含 INJECT_EVENTS 權限
├── build打包.bat                       ← 一鍵編譯
├── 安裝系統App.bat                     ← 一鍵安裝 (自動找 IP)
├── url.txt                            ← 頻道網址列表
└── WIKI.md                            ← 本文件
```

---

## 除錯日誌

查看自動點擊日誌：
```batch
adb logcat -s "WebViewActivity"
```

預期輸出：
```
D/WebViewActivity: Starting grid switch sequence...
D/WebViewActivity: Click 1: 2-grid → 4-grid
D/WebViewActivity: injectTap DOWN at (1080.0, 18.0) OK
D/WebViewActivity: injectTap UP at (1080.0, 18.0) OK
D/WebViewActivity: Click 2: 4-grid → 6-grid
D/WebViewActivity: injectTap DOWN at (1080.0, 18.0) OK
D/WebViewActivity: injectTap UP at (1080.0, 18.0) OK
```

確認系統 App 狀態：
```batch
adb shell "dumpsys package tvbox.kj | grep pkgFlags"
```

預期輸出：
```
pkgFlags=[ SYSTEM DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP PRIVILEGED ]
```

---

## 已知限制

| 問題 | 說明 |
|------|------|
| `pm install` 失效 | 此電視盒只能用 `su 0 cp` 複製 APK |
| `su` 從 App 內部失敗 | Android 核心阻止 untrusted_app 的 setuid 轉換 |
| `dispatchGesture()` 不可用 | 需要 API 24+，此設備實際 API 19 |
| `dispatchTouchEvent` 無效 | X5 WebView 不轉發非系統級觸摸到 Canvas |
| 設備版本偽造 | 報告 Android 11 但實際是 Android 4.4 |

---

## 技術決策記錄

| 方案 | 結果 | 原因 |
|------|------|------|
| JS 注入點擊 | 失敗 | Laya Canvas 無 DOM 元素 |
| `dispatchTouchEvent` | 失敗 | X5 不轉發到 Canvas |
| `su` 提權 | 失敗 | 核心阻止 setuid |
| AccessibilityService | 失敗 | `dispatchGesture` 需 API 24+ |
| Auto.js | 失敗 | 底層依賴 `dispatchGesture` |
| `/system/app/` | 失敗 | 未授予 INJECT_EVENTS |
| **`/system/priv-app/`** | **成功** | 獲得 PRIVILEGED 權限 |

---

*最後更新：2026-09-09*
