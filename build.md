# TVBOX無按鈕 專案編譯痛點與解決方案報告

本報告總結了在 Windows 環境下編譯 `TVBOX無按鈕` 專案為 APK 時遇到的主要痛點（陷阱）以及對應的解決方案。這些問題主要肇因於**專案路徑中包含中文字元**（非 ASCII 字元）。

## 環境需求與相關路徑
- **專案根目錄**: `d:\TVBOX無按鈕`
- **Java 環境變數 (JAVA_HOME)**: 必須設定為 `D:\java`
- **Java 執行檔路徑**: `D:\java\bin\java.exe`
- **Gradle Wrapper 路徑**: `d:\TVBOX無按鈕\gradle\wrapper\gradle-wrapper.jar`
- **編譯輸出 APK 路徑**: `d:\TVBOX無按鈕\app\build\outputs\apk\release\app-armeabi-v7a-release-unsigned.apk`

## 痛點一：`gradlew.bat` 路徑解析錯誤 (字碼亂碼)

### 發生原因
當執行 `build.bat` 或 `gradlew.bat` 時，批次檔內部會使用 `%~dp0` 來動態獲取當前目錄的絕對路徑，並將其作為引數傳遞給 Java 以啟動 Gradle Wrapper (即 `gradle-wrapper.jar`)。
由於專案路徑 `d:\TVBOX無按鈕` 包含了中文字元，在 Windows CMD 環境下，如果系統字碼頁 (Code Page) 設定無法正確處理 UTF-8，中文字元在傳遞時就會變成亂碼（例如 `D:\TVBOX????\gradle\wrapper\gradle-wrapper.jar`），導致 Java 拋出 `找不到或無法載入主類別` 或執行緒啟動錯誤。

### 解決方案
**繞過 `gradlew.bat`，直接使用 Java 執行 Gradle 並使用相對路徑**。
我們透過 PowerShell 環境，直接指定 `java.exe` 的絕對路徑，並傳入相對路徑的 `gradle-wrapper.jar`：
```powershell
$env:JAVA_HOME="D:\java"
& "D:\java\bin\java.exe" "-Dorg.gradle.appname=gradlew" "-classpath" "gradle\wrapper\gradle-wrapper.jar" "org.gradle.wrapper.GradleWrapperMain" "assembleRelease"
```
這樣可以避免絕對路徑引發的編碼解析問題。

## 痛點二：Android Gradle 外掛的非 ASCII 路徑阻擋機制

### 發生原因
Android 的編譯工具 (Android Gradle Plugin, AGP) 內建了一個安全檢查機制。當偵測到專案位於包含「非 ASCII 字元（如中文）」的路徑時，會直接中斷編譯並拋出錯誤：
> `Your project path contains non-ASCII characters. This will most likely cause the build to fail on Windows. Please move your project to a different directory.`

這是因為在 Windows 環境下，非英文路徑有極高機率導致 NDK/JNI 編譯失敗或資源打包時發生讀取異常。

### 解決方案
**強制覆蓋路徑安全檢查**。
我們修改了專案目錄底下的 `gradle.properties`，在檔案最末端加入官方提供的實驗性忽略參數，強制允許在中文目錄下進行編譯：
```properties
android.overridePathCheck=true
```

## 總結與建議
雖然目前已經透過上述兩種繞道方式（直接呼叫 Java 搭配相對路徑、修改 gradle 配置）成功編譯出 APK，但長遠來看，強烈建議**將專案移動到全英文路徑的資料夾**（例如 `D:\Workspace\TVBoxNoButton`）。這樣不僅可以直接點擊 `build.bat` 成功編譯，也能避免未來引入第三方 C/C++ 函式庫或複雜資源時發生無法預期的不明錯誤。

## APP 開啟的網址修改方式
如果您需要在編譯前更改 APP 預設開啟的網頁網址，請修改以下原始碼檔案：

**檔案路徑**：
`app\src\main\java\com\example\bingo\MainActivity.kt`

**修改方式**：
尋找大約在第 9 行的位置，會看到宣告網址的變數：
```kotlin
private val fixedUrl = "https://lotto.auzonet.com/tv/comboTv/"
```
只需將引號內的 `https://lotto.auzonet.com/tv/comboTv/` 替換為您想要的新網址，然後重新執行編譯步驟即可生效。
