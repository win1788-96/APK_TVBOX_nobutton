@echo off
echo ========================================================
echo Starting TVBOX APP Compilation (Bypassing Unicode Path)
echo ========================================================
set "JAVA_HOME=D:\java"
cd /d "%~dp0"

echo [Status] Running Gradle wrapper directly with Java...
"%JAVA_HOME%\bin\java.exe" "-Dorg.gradle.appname=gradlew" "-classpath" "gradle\wrapper\gradle-wrapper.jar" "org.gradle.wrapper.GradleWrapperMain" "assembleDebug"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo  [SUCCESS] APK generated and signed!
    echo  [APK Path] app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo  [FAILED] Build failed. Please scroll up to see errors.
    echo ========================================================
)

pause