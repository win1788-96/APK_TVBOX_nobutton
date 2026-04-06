# 電視盒 Legacy 專案 (API 19 相容版) 📺

本專案是專為 Android 4.4 (KitKat) 舊型電視盒（如 HiSTB）優化的 IPTV/網頁導航應用程式。為了確保在老舊硬體上的穩定運行與現代網頁支援，本專案已全面從 Jetpack Compose 遷移至 **傳統 XML View + 騰訊 X5 核心** 架构。

## 🌟 核心特色

- **跨世代相容性**：最低支援 Android 4.4 (API 19)，解決現代 Android 框架無法在舊機型安裝的問題。
- **現代渲染能力**：內建 **騰訊 TBS (X5 Core)**，提供 Chromium 等級的瀏覽能力，完美解決老舊系統 WebView 的白畫面與 SSL 錯誤。
- **TV 遙控器優化**：主選單採用 `GridView` 佈局，特大字體 (40sp) 與圓角卡片設計，適合 D-Pad 遙控器操作。
- **智慧 WebView**：
    - 支援 HTTPS 安全憑證略過（解決舊機憑證過期問題）。
    - 右下角快速旋轉按鈕，點擊畫面 3 秒後自動隱藏。
    - 整合 JS 注入，自動處理網頁邊距與全螢幕顯示。

## 🛠️ 技術規格

- **UI 框架**：Android XML Views / AppCompat (Legacy)
- **最低 SDK**：19 (Android 4.4)
- **目標 SDK**：34 (Android 14)
- **瀏覽核心**：Tencent TBS / X5 (com.tencent.smtt.sdk)
- **資料來源**：Google Sheets CSV 動態加載
- **部署方式**：Root-level 實體路徑強制安裝

## 🚀 部署教學 (以 HiSTB 為例)

由於舊型電視盒的 `pm install` 經常失效，建議使用 Root 強制安裝法：

1. **編譯 APK**：
   ```bash
   ./gradlew assembleDebug
   ```
2. **手動推送並覆寫**：
   ```bash
   # 推送到暫存區
   adb push app-debug.apk /data/local/tmp/bingo.apk
   # 強制搬移至 App 目錄 (需要 Root)
   adb shell "cp /data/local/tmp/bingo.apk /data/app/com.example.bingo-1.apk"
   # 設定權限
   adb shell "chmod 644 /data/app/com.example.bingo-1.apk; chown system:system /data/app/com.example.bingo-1.apk"
   # 重啟設備生效
   adb reboot
   ```

## 📂 專案結構

- `MainActivity.kt`: 負責 CSV 解析與選單 GridView 建立。
- `WebViewActivity.kt`: 實作 X5 快顯引擎與旋轉控制邏輯。
- `res/layout`: 傳統計量版 XML 佈局檔案。
- `gradle.properties`: 已開啟 AndroidX 與 Jetifier 相容模式。

---
*本專案由 Antigravity AI 協助完成，針對 Legacy Android 硬體進行全方位優化。*
