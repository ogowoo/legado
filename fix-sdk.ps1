# 快速修复 SDK 配置
param()

$ErrorActionPreference = "Stop"

# 创建 SDK 目录
$SdkPath = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force -Path $SdkPath | Out-Null

# 下载命令行工具
$Url = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$ZipFile = "$env:TEMP\android-cmdline-tools.zip"

Write-Host "下载命令行工具..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $Url -OutFile $ZipFile -UseBasicParsing

# 解压
Write-Host "解压工具..." -ForegroundColor Cyan
Expand-Archive -Path $ZipFile -DestinationPath "$env:TEMP\android-temp" -Force

# 移动
$CmdlineToolsPath = "$SdkPath\cmdline-tools"
New-Item -ItemType Directory -Force -Path $CmdlineToolsPath | Out-Null

if (Test-Path "$CmdlineToolsPath\latest") {
    Remove-Item -Path "$CmdlineToolsPath\latest" -Recurse -Force
}

Move-Item -Path "$env:TEMP\android-temp\cmdline-tools" -Destination "$CmdlineToolsPath\latest" -Force

# 设置环境变量并安装
$env:ANDROID_HOME = $SdkPath
$env:ANDROID_SDK_ROOT = $SdkPath

Write-Host "安装 SDK 组件..." -ForegroundColor Cyan
$SdkManager = "$CmdlineToolsPath\latest\bin\sdkmanager.bat"

# 先接受许可证
echo "y" | & $SdkManager --licenses 2>&1 | Out-Null

# 安装组件
& $SdkManager --install "platforms;android-36" "platform-tools" "build-tools;36.0.0" 2>&1

Write-Host "SDK 路径: $SdkPath" -ForegroundColor Green

# 设置永久环境变量
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkPath, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkPath, "User")

# 添加到 PATH
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
$NewPaths = @(
    "$SdkPath\cmdline-tools\latest\bin"
    "$SdkPath\platform-tools"
)
foreach ($Path in $NewPaths) {
    if ($UserPath -notlike "*$Path*") {
        $UserPath = "$UserPath;$Path"
    }
}
[Environment]::SetEnvironmentVariable("Path", $UserPath, "User")

# 修复 local.properties
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LocalPropertiesPath = "$ProjectDir\local.properties"
$EscapedPath = $SdkPath -replace '\\', '\\'
"sdk.dir=$EscapedPath" | Set-Content -Path $LocalPropertiesPath -Force

Write-Host "已更新 local.properties" -ForegroundColor Green
Write-Host ""
Write-Host "请重新打开 PowerShell，然后运行:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat assembleDebug" -ForegroundColor Cyan
