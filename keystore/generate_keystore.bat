@echo off
chcp 65001

REM 生成读书应用签名密钥库

echo ==========================================
echo  读书应用签名密钥生成工具
echo ==========================================
echo.

set KEYSTORE_FILE=dushu.keystore
set ALIAS=dushu
set VALIDITY=36500

if exist %KEYSTORE_FILE% (
    echo 警告: %KEYSTORE_FILE% 已存在！
    set /p OVERWRITE="是否覆盖? (y/n): "
    if /i not "%OVERWRITE%"=="y" exit /b 1
    del %KEYSTORE_FILE%
)

echo 正在生成密钥库...
keytool -genkey -v -keystore %KEYSTORE_FILE% -alias %ALIAS% -keyalg RSA -keysize 2048 -validity %VALIDITY% -storepass dushu123456 -keypass dushu123456 -dname "CN=读书, OU=读书应用, O=读书团队, L=中国, ST=北京, C=CN"

if %errorlevel% neq 0 (
    echo 错误: 生成密钥库失败！
    pause
    exit /b 1
)

echo.
echo ==========================================
echo  密钥库生成成功！
echo ==========================================
echo.
echo 文件: %KEYSTORE_FILE%
echo 别名: %ALIAS%
echo 密码: dushu123456
echo.
echo 重要: 请妥善保管此密钥库文件和密码！
echo.
keytool -list -v -keystore %KEYSTORE_FILE% -storepass dushu123456
pause
